package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jdbc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.CommandStatusView;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CommandStatusRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public CommandStatusRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void markAppliedFromEvent(OutboxEventJpaEntity event) {
        extractCommandId(event.getPayloadJson()).ifPresent(commandId -> {
            Instant appliedAt = event.getOccurredAt() != null ? event.getOccurredAt() : Instant.now();
            jdbcTemplate.update("""
                    INSERT INTO command_status (
                        command_id, status, aggregate_type, aggregate_id,
                        event_id, event_type, applied_at, rejected_at, reason, updated_at
                    )
                    VALUES (?, 'APPLIED', ?, ?, ?, ?, ?, NULL, NULL, ?)
                    ON CONFLICT (command_id) DO UPDATE
                    SET status = 'APPLIED',
                        aggregate_type = EXCLUDED.aggregate_type,
                        aggregate_id = EXCLUDED.aggregate_id,
                        event_id = EXCLUDED.event_id,
                        event_type = EXCLUDED.event_type,
                        applied_at = EXCLUDED.applied_at,
                        rejected_at = NULL,
                        reason = NULL,
                        updated_at = EXCLUDED.updated_at
                    """,
                    commandId,
                    event.getAggregateType(),
                    event.getAggregateId(),
                    event.getEventId(),
                    event.getEventType(),
                    Timestamp.from(appliedAt),
                    Timestamp.from(Instant.now()));
        });
    }

    public CommandStatusView find(UUID commandId) {
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT status, applied_at, rejected_at, reason
                    FROM command_status
                    WHERE command_id = ?
                    """,
                    (rs, rowNum) -> new CommandStatusView(
                            rs.getString("status"),
                            rs.getTimestamp("applied_at") != null ? rs.getTimestamp("applied_at").toInstant() : null,
                            rs.getTimestamp("rejected_at") != null ? rs.getTimestamp("rejected_at").toInstant() : null,
                            rs.getString("reason")),
                    commandId);
        } catch (EmptyResultDataAccessException e) {
            return new CommandStatusView("PENDING", null, null, null);
        }
    }

    private Optional<UUID> extractCommandId(String payloadJson) {
        try {
            JsonNode root = objectMapper.readTree(payloadJson);
            if (!root.hasNonNull("commandId")) {
                return Optional.empty();
            }
            return Optional.of(UUID.fromString(root.get("commandId").asText()));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }
}
