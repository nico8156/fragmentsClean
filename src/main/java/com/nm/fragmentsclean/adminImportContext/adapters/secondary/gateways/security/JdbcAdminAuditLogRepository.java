package com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.AdminAuditEntry;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.AdminAuditLogRepository;

@Repository
public class JdbcAdminAuditLogRepository implements AdminAuditLogRepository {
	private final JdbcTemplate jdbcTemplate;
	public JdbcAdminAuditLogRepository(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }
	@Override public void append(AdminAuditEntry entry) {
		jdbcTemplate.update("""
				INSERT INTO admin_audit_log (id, actor_user_id, action, target_type, target_id, target_user_id, command_id, outcome, occurred_at)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
				""", entry.id(), entry.actorUserId(), entry.action(), entry.targetType(), entry.targetId(),
				entry.targetType().equals("USER") ? entry.targetId() : null, entry.commandId(), entry.outcome(), Timestamp.from(entry.occurredAt()));
	}
}
