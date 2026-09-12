package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jdbc;

import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

@Repository
public class InboxMessageRepository {

    private final JdbcTemplate jdbcTemplate;
    private final Duration claimLease;

    public InboxMessageRepository(JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, Duration.ofMinutes(5));
    }

    InboxMessageRepository(JdbcTemplate jdbcTemplate, Duration claimLease) {
        this.jdbcTemplate = jdbcTemplate;
        this.claimLease = claimLease;
    }

    public boolean claim(IntegrationEventEnvelope envelope) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO inbox_messages (
                        destination, event_id, event_type, event_version,
                        received_at, status, lease_until
                    )
                    VALUES (?, ?, ?, ?, ?, 'RECEIVED', ?)
                    """,
                    envelope.destination(),
                    envelope.eventId(),
                    envelope.eventType(),
                    envelope.eventVersion(),
                    Timestamp.from(Instant.now()),
                    Timestamp.from(Instant.now().plus(claimLease)));
            return true;
        } catch (DuplicateKeyException duplicate) {
            Instant now = Instant.now();
            return jdbcTemplate.update("""
                    UPDATE inbox_messages
                    SET status = 'RECEIVED',
                        received_at = ?,
                        lease_until = ?,
                        processed_at = NULL,
                        error_message = NULL
                    WHERE destination = ?
                      AND event_id = ?
                      AND (status = 'FAILED'
                           OR (status = 'RECEIVED' AND (lease_until IS NULL OR lease_until <= ?)))
                    """,
                    Timestamp.from(now),
                    Timestamp.from(now.plus(claimLease)),
                    envelope.destination(),
                    envelope.eventId(),
                    Timestamp.from(now)) == 1;
        }
    }

    public void markProcessed(IntegrationEventEnvelope envelope) {
        jdbcTemplate.update("""
                UPDATE inbox_messages
                SET status = 'PROCESSED',
                    processed_at = ?,
                    lease_until = NULL,
                    error_message = NULL
                WHERE destination = ?
                  AND event_id = ?
                """,
                Timestamp.from(Instant.now()),
                envelope.destination(),
                envelope.eventId());
    }

    public void markFailed(IntegrationEventEnvelope envelope, Exception error) {
        jdbcTemplate.update("""
                UPDATE inbox_messages
                SET status = 'FAILED',
                    lease_until = NULL,
                    error_message = ?
                WHERE destination = ?
                  AND event_id = ?
                """,
                error.getMessage(),
                envelope.destination(),
                envelope.eventId());
    }
}
