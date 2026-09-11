package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jdbc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.*;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CommandStatusRepository implements CommandStatusRecorder, CommandReceiptStore, CommandStatusReader {
    private static final CommandStatusView PENDING_VIEW = new CommandStatusView("PENDING", null, null, null, null);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public CommandStatusRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public void markAppliedFromEvent(OutboxEventJpaEntity event) {
        extractCommandId(event.getPayloadJson()).ifPresent(commandId -> {
            Instant appliedAt = event.getOccurredAt() != null ? event.getOccurredAt() : Instant.now();
            markAppliedFromEvent(commandId, event.getAggregateType(), event.getAggregateId(),
                    event.getEventId(), event.getEventType(), appliedAt);
        });
    }

    @Override
    public void markApplied(UUID commandId, String aggregateType, String aggregateId, String eventType, Instant appliedAt) {
        markAppliedFromEvent(commandId, aggregateType, aggregateId, null, eventType, appliedAt);
    }

    @Override
    public boolean isApplied(UUID commandId) {
        return "APPLIED".equals(find(commandId).status());
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CommandReceipt registerPending(CommandDescriptor descriptor, Instant now) {
        jdbcTemplate.update("""
                INSERT INTO command_status (
                    command_id, requester_id, command_type, fingerprint, status,
                    aggregate_type, aggregate_id, event_id, event_type,
                    applied_at, rejected_at, rejection_code, reason, updated_at
                )
                VALUES (?, ?, ?, ?, 'PENDING', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, ?)
                ON CONFLICT (command_id) DO NOTHING
                """,
                descriptor.commandId(), descriptor.requesterId(), descriptor.commandType(), descriptor.fingerprint(),
                Timestamp.from(now));
        return receipt(descriptor.commandId(), false);
    }

    @Override
    public CommandReceipt lock(CommandDescriptor descriptor) {
        return receipt(descriptor.commandId(), true);
    }

    @Override
    public void markApplied(CommandDescriptor descriptor, Instant appliedAt) {
        int updated = jdbcTemplate.update("""
                UPDATE command_status
                SET status = 'APPLIED', applied_at = ?, rejected_at = NULL,
                    rejection_code = NULL, reason = NULL, updated_at = ?
                WHERE command_id = ? AND requester_id = ? AND command_type = ? AND fingerprint = ?
                """,
                Timestamp.from(appliedAt), Timestamp.from(appliedAt), descriptor.commandId(),
                descriptor.requesterId(), descriptor.commandType(), descriptor.fingerprint());
        requireMatchingReceipt(updated);
    }

    @Override
    public void markRejected(CommandDescriptor descriptor, String rejectionCode, String reason, Instant rejectedAt) {
        int updated = jdbcTemplate.update("""
                UPDATE command_status
                SET status = 'REJECTED', applied_at = NULL, rejected_at = ?,
                    rejection_code = ?, reason = ?, updated_at = ?
                WHERE command_id = ? AND requester_id = ? AND command_type = ? AND fingerprint = ?
                  AND status <> 'APPLIED'
                """,
                Timestamp.from(rejectedAt), rejectionCode, reason, Timestamp.from(rejectedAt),
                descriptor.commandId(), descriptor.requesterId(), descriptor.commandType(), descriptor.fingerprint());
        requireMatchingReceipt(updated);
    }

    @Override
    public CommandStatusView find(UUID commandId) {
        return findView(commandId, null, false);
    }

    @Override
    public CommandStatusView findForRequester(UUID commandId, UUID requesterId) {
        return findView(commandId, requesterId, true);
    }

    private void markAppliedFromEvent(UUID commandId, String aggregateType, String aggregateId,
                                      String eventId, String eventType, Instant appliedAt) {
        jdbcTemplate.update("""
                INSERT INTO command_status (
                    command_id, requester_id, command_type, fingerprint, status,
                    aggregate_type, aggregate_id, event_id, event_type,
                    applied_at, rejected_at, rejection_code, reason, updated_at
                )
                VALUES (?, NULL, NULL, NULL, 'APPLIED', ?, ?, ?, ?, ?, NULL, NULL, NULL, ?)
                ON CONFLICT (command_id) DO UPDATE
                SET aggregate_type = EXCLUDED.aggregate_type,
                    aggregate_id = EXCLUDED.aggregate_id,
                    event_id = COALESCE(EXCLUDED.event_id, command_status.event_id),
                    event_type = EXCLUDED.event_type,
                    applied_at = CASE WHEN command_status.status = 'REJECTED'
                                      THEN command_status.applied_at ELSE EXCLUDED.applied_at END,
                    updated_at = CASE WHEN command_status.status = 'REJECTED'
                                      THEN command_status.updated_at ELSE EXCLUDED.updated_at END,
                    status = CASE WHEN command_status.status = 'REJECTED'
                                  THEN command_status.status ELSE 'APPLIED' END
                """,
                commandId, aggregateType, aggregateId, eventId, eventType,
                Timestamp.from(appliedAt), Timestamp.from(Instant.now()));
    }

    private CommandReceipt receipt(UUID commandId, boolean forUpdate) {
        String suffix = forUpdate ? " FOR UPDATE" : "";
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT command_id, requester_id, command_type, fingerprint, status,
                           applied_at, rejected_at, rejection_code, reason
                    FROM command_status
                    WHERE command_id = ?
                    """ + suffix, (rs, rowNum) -> mapReceipt(rs), commandId);
        } catch (EmptyResultDataAccessException missing) {
            throw new IllegalStateException("Command receipt disappeared during execution", missing);
        }
    }

    private CommandStatusView findView(UUID commandId, UUID requesterId, boolean ownerFiltered) {
        try {
            String ownerClause = ownerFiltered ? " AND requester_id = ?" : "";
            Object[] args = ownerFiltered ? new Object[]{commandId, requesterId} : new Object[]{commandId};
            return jdbcTemplate.queryForObject("""
                    SELECT status, applied_at, rejected_at, rejection_code, reason
                    FROM command_status
                    WHERE command_id = ?
                    """ + ownerClause,
                    (rs, rowNum) -> new CommandStatusView(
                            rs.getString("status"), instant(rs, "applied_at"), instant(rs, "rejected_at"),
                            rs.getString("rejection_code"), rs.getString("reason")), args);
        } catch (EmptyResultDataAccessException missingOrForeign) {
            return PENDING_VIEW;
        }
    }

    private CommandReceipt mapReceipt(ResultSet rs) throws SQLException {
        var descriptor = new CommandDescriptor(
                rs.getObject("command_id", UUID.class),
                rs.getObject("requester_id", UUID.class),
                rs.getString("command_type"),
                rs.getString("fingerprint"));
        return new CommandReceipt(descriptor, CommandReceiptStatus.valueOf(rs.getString("status")),
                instant(rs, "applied_at"), instant(rs, "rejected_at"),
                rs.getString("rejection_code"), rs.getString("reason"));
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private void requireMatchingReceipt(int updated) {
        if (updated != 1) {
            throw new CommandIdentityConflictException();
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
