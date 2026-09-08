package com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.AdminAuditEntry;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.AdminAuditLogRepository;

@Repository
public class JdbcAdminAuditLogRepository implements AdminAuditLogRepository {
	private final JdbcTemplate jdbcTemplate;
	public JdbcAdminAuditLogRepository(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }
	@Override public void append(AdminAuditEntry entry) {
		jdbcTemplate.update("""
				INSERT INTO admin_audit_log (id, actor_user_id, action, target_type, target_id, target_user_id, command_id, outcome, reason, occurred_at)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
				""", entry.id(), entry.actorUserId(), entry.action(), entry.targetType(), entry.targetId(),
				entry.targetType().equals("USER") ? entry.targetId() : null, entry.commandId(), entry.outcome(), entry.reason(), Timestamp.from(entry.occurredAt()));
	}
	@Override
	public List<AdminAuditEntry> findByTarget(String targetType, UUID targetId, int limit) {
		return jdbcTemplate.query("""
				SELECT id, actor_user_id, action, target_type, target_id, command_id, outcome, reason, occurred_at
				FROM admin_audit_log
				WHERE target_type = ? AND target_id = ?
				ORDER BY occurred_at DESC, id DESC
				LIMIT ?
				""", (rs, rowNumber) -> new AdminAuditEntry(
				UUID.fromString(rs.getString("id")),
				UUID.fromString(rs.getString("actor_user_id")),
				rs.getString("action"), rs.getString("target_type"),
				UUID.fromString(rs.getString("target_id")),
				rs.getObject("command_id", UUID.class), rs.getString("outcome"), rs.getString("reason"),
				rs.getTimestamp("occurred_at").toInstant()), targetType, targetId, limit);
	}
}
