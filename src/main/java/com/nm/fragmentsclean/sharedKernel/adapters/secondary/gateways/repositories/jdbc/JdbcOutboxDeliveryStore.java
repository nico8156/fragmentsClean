package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jdbc;

import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.OutboxMessage;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.gateways.OutboxDeliveryStore;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** PostgreSQL claim/lease adapter. No network call is made from this adapter. */
public final class JdbcOutboxDeliveryStore implements OutboxDeliveryStore {
    private static final int MAX_ERROR_LENGTH = 1_000;

    private final JdbcTemplate jdbc;
    private final CommandStatusRepository commandStatuses;
    private final TransactionTemplate transactions;

    public JdbcOutboxDeliveryStore(JdbcTemplate jdbc, CommandStatusRepository commandStatuses,
                                   PlatformTransactionManager transactionManager) {
        this.jdbc = jdbc;
        this.commandStatuses = commandStatuses;
        this.transactions = new TransactionTemplate(transactionManager);
    }

    @Override
    public List<OutboxMessage> claimDue(String owner, Instant now, Instant leaseUntil, int limit) {
        if (limit < 1) throw new IllegalArgumentException("Outbox claim limit must be positive");
        return transactions.execute(status -> jdbc.query("""
                WITH candidates AS (
                    SELECT candidate.id
                    FROM outbox_events candidate
                    WHERE candidate.status = 'PENDING'
                      AND COALESCE(candidate.next_attempt_at, candidate.created_at) <= ?
                      AND (candidate.lease_until IS NULL OR candidate.lease_until <= ?)
                      AND NOT EXISTS (
                          SELECT 1
                          FROM outbox_events predecessor
                          WHERE predecessor.stream_key = candidate.stream_key
                            AND predecessor.id < candidate.id
                            AND predecessor.status <> 'SENT'
                      )
                    ORDER BY candidate.id
                    FOR UPDATE SKIP LOCKED
                    LIMIT ?
                ), claimed AS (
                    UPDATE outbox_events event
                    SET lease_owner = ?, lease_until = ?
                    FROM candidates
                    WHERE event.id = candidates.id
                    RETURNING event.id, event.event_id, event.event_type,
                              event.aggregate_type, event.aggregate_id, event.stream_key,
                              event.payload_json, event.occurred_at, event.created_at,
                              event.retry_count
                )
                SELECT claimed.id, claimed.event_id, claimed.event_type,
                       claimed.aggregate_type, claimed.aggregate_id, claimed.stream_key,
                       CASE
                           WHEN claimed.payload_json ~ '^[0-9]+$'
                            AND EXISTS (
                                SELECT 1 FROM pg_largeobject_metadata legacy
                                WHERE legacy.oid = claimed.payload_json::oid
                            )
                           THEN convert_from(lo_get(claimed.payload_json::oid), 'UTF8')
                           ELSE claimed.payload_json
                       END AS payload_json,
                       claimed.occurred_at, claimed.created_at, claimed.retry_count
                FROM claimed
                ORDER BY claimed.id
                """, this::map, Timestamp.from(now), Timestamp.from(now), limit,
                owner, Timestamp.from(leaseUntil)));
    }

    @Override
    public boolean markSent(OutboxMessage message, String owner, Instant completedAt) {
        Boolean completed = transactions.execute(status -> {
            int updated = jdbc.update("""
                    UPDATE outbox_events
                    SET status = 'SENT', lease_owner = NULL, lease_until = NULL,
                        next_attempt_at = NULL, last_error = NULL
                    WHERE id = ? AND status = 'PENDING' AND lease_owner = ?
                    """, message.id(), owner);
            if (updated == 1) {
                commandStatuses.markAppliedFromEvent(message, completedAt);
                return true;
            }
            return false;
        });
        return Boolean.TRUE.equals(completed);
    }

    @Override
    public boolean recordFailure(long id, String owner, int failureCount, Instant nextAttemptAt,
                                 String errorMessage, boolean terminal) {
        Boolean recorded = transactions.execute(status -> jdbc.update("""
                UPDATE outbox_events
                SET status = ?, retry_count = ?, next_attempt_at = ?, last_error = ?,
                    lease_owner = NULL, lease_until = NULL
                WHERE id = ? AND status = 'PENDING' AND lease_owner = ?
                """, terminal ? "FAILED" : "PENDING", failureCount,
                terminal ? null : Timestamp.from(nextAttemptAt), bounded(errorMessage), id, owner) == 1);
        return Boolean.TRUE.equals(recorded);
    }

    private OutboxMessage map(ResultSet rs, int rowNumber) throws SQLException {
        return new OutboxMessage(
                rs.getLong("id"), rs.getString("event_id"), rs.getString("event_type"),
                rs.getString("aggregate_type"), rs.getString("aggregate_id"),
                rs.getString("stream_key"), rs.getString("payload_json"),
                rs.getTimestamp("occurred_at").toInstant(), rs.getTimestamp("created_at").toInstant(),
                rs.getInt("retry_count"));
    }

    private String bounded(String message) {
        if (message == null) return null;
        return message.length() <= MAX_ERROR_LENGTH ? message : message.substring(0, MAX_ERROR_LENGTH);
    }
}
