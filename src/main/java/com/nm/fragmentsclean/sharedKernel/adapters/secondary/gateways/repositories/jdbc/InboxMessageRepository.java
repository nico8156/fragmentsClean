package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jdbc;

import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;

@Repository
public class InboxMessageRepository {

    private final JdbcTemplate jdbcTemplate;

    public InboxMessageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean claim(IntegrationEventEnvelope envelope) {
        try {
            jdbcTemplate.update("""
                    INSERT INTO inbox_messages (
                        destination, event_id, event_type, event_version,
                        received_at, status
                    )
                    VALUES (?, ?, ?, ?, ?, 'RECEIVED')
                    """,
                    envelope.destination(),
                    envelope.eventId(),
                    envelope.eventType(),
                    envelope.eventVersion(),
                    Timestamp.from(Instant.now()));
            return true;
        } catch (DuplicateKeyException duplicate) {
            return false;
        }
    }

    public void markProcessed(IntegrationEventEnvelope envelope) {
        jdbcTemplate.update("""
                UPDATE inbox_messages
                SET status = 'PROCESSED',
                    processed_at = ?,
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
                    error_message = ?
                WHERE destination = ?
                  AND event_id = ?
                """,
                error.getMessage(),
                envelope.destination(),
                envelope.eventId());
    }
}
