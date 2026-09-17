package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jdbc;

import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.InboxClaim;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.InboxMessageStore;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Repository
public class InboxMessageRepository implements InboxMessageStore {

    private final JdbcTemplate jdbcTemplate;
    private final Duration claimLease;

    @Autowired
    public InboxMessageRepository(JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, Duration.ofMinutes(5));
    }

    InboxMessageRepository(JdbcTemplate jdbcTemplate, Duration claimLease) {
        this.jdbcTemplate = jdbcTemplate;
        this.claimLease = claimLease;
    }

    @Override
    public InboxClaim claim(IntegrationEventEnvelope envelope) {
        Instant now = Instant.now();
        Instant leaseUntil = now.plus(claimLease);
        String ownerToken = UUID.randomUUID().toString();
        try {
            jdbcTemplate.update("""
                    INSERT INTO inbox_messages (
                        destination, event_id, event_type, event_version,
                        received_at, status, lease_until, lease_owner
                    )
                    VALUES (?, ?, ?, ?, ?, 'RECEIVED', ?, ?)
                    """,
                    envelope.destination(),
                    envelope.eventId(),
                    envelope.eventType(),
                    envelope.eventVersion(),
                    Timestamp.from(now),
                    Timestamp.from(leaseUntil),
                    ownerToken);
            return InboxClaim.acquired(ownerToken, leaseUntil);
        } catch (DuplicateKeyException duplicate) {
            int updated = jdbcTemplate.update("""
                    UPDATE inbox_messages
                    SET status = 'RECEIVED',
                        received_at = ?,
                        lease_until = ?,
                        lease_owner = ?,
                        processed_at = NULL,
                        error_message = NULL
                    WHERE destination = ?
                      AND event_id = ?
                      AND (status = 'FAILED'
                           OR (status = 'RECEIVED' AND (lease_until IS NULL OR lease_until <= ?)))
                    """,
                    Timestamp.from(now),
                    Timestamp.from(leaseUntil),
                    ownerToken,
                    envelope.destination(),
                    envelope.eventId(),
                    Timestamp.from(now));
            if (updated == 1) {
                return InboxClaim.acquired(ownerToken, leaseUntil);
            }
            return currentClaimState(envelope);
        }
    }

    @Override
    public boolean markProcessed(IntegrationEventEnvelope envelope, String ownerToken) {
        return jdbcTemplate.update("""
                UPDATE inbox_messages
                SET status = 'PROCESSED',
                    processed_at = ?,
                    lease_until = NULL,
                    lease_owner = NULL,
                    error_message = NULL
                WHERE destination = ?
                  AND event_id = ?
                  AND status = 'RECEIVED'
                  AND lease_owner = ?
                """,
                Timestamp.from(Instant.now()),
                envelope.destination(),
                envelope.eventId(),
                ownerToken) == 1;
    }

    @Override
    public boolean markFailed(IntegrationEventEnvelope envelope, String ownerToken, Exception error) {
        return jdbcTemplate.update("""
                UPDATE inbox_messages
                SET status = 'FAILED',
                    lease_until = NULL,
                    lease_owner = NULL,
                    error_message = ?
                WHERE destination = ?
                  AND event_id = ?
                  AND status = 'RECEIVED'
                  AND lease_owner = ?
                """,
                error.getMessage(),
                envelope.destination(),
                envelope.eventId(),
                ownerToken) == 1;
    }

    private InboxClaim currentClaimState(IntegrationEventEnvelope envelope) {
        var states = jdbcTemplate.query("""
                SELECT status, lease_until
                FROM inbox_messages
                WHERE destination = ?
                  AND event_id = ?
                """,
                (rs, rowNumber) -> new ExistingClaim(rs.getString("status"), rs.getTimestamp("lease_until")),
                envelope.destination(),
                envelope.eventId());
        if (states.size() != 1) {
            throw new IllegalStateException("Inbox claim state disappeared for event " + envelope.eventId());
        }
        ExistingClaim state = states.getFirst();
        if ("PROCESSED".equals(state.status())) {
            return InboxClaim.alreadyProcessed();
        }
        if ("RECEIVED".equals(state.status()) && state.leaseUntil() != null) {
            return InboxClaim.busyUntil(state.leaseUntil().toInstant());
        }
        throw new IllegalStateException("Unexpected inbox claim state " + state.status()
                + " for event " + envelope.eventId());
    }

    private record ExistingClaim(String status, Timestamp leaseUntil) {}
}
