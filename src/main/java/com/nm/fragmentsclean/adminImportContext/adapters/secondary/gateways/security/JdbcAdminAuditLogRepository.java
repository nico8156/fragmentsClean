package com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.sql.Timestamp;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.AdminAuditEntry;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.AdminAuditCursor;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.AdminAuditPage;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.AdminAuditReadRepository;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.SearchAdminAuditQuery;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.AdminAuditLogRepository;

@Repository
public class JdbcAdminAuditLogRepository implements AdminAuditLogRepository, AdminAuditReadRepository {
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

    @Override
    public AdminAuditPage search(SearchAdminAuditQuery query) {
        var sql = new StringBuilder("""
                SELECT id, actor_user_id, action, target_type, target_id, command_id, outcome, reason, occurred_at
                FROM admin_audit_log WHERE TRUE
                """);
        var args = new ArrayList<Object>();
        if (query.targetType() != null) { sql.append(" AND target_type=?"); args.add(query.targetType()); }
        if (query.targetId() != null) { sql.append(" AND target_id=?"); args.add(query.targetId()); }
        if (query.actorId() != null) { sql.append(" AND actor_user_id=?"); args.add(query.actorId()); }
        if (query.commandId() != null) { sql.append(" AND command_id=?"); args.add(query.commandId()); }
        if (query.action() != null) { sql.append(" AND action=?"); args.add(query.action()); }
        if (query.outcome() != null) { sql.append(" AND outcome=?"); args.add(query.outcome()); }
        if (query.cursor() != null) {
            sql.append(" AND (occurred_at,id) < (?,?)");
            args.add(Timestamp.from(query.cursor().occurredAt())); args.add(query.cursor().id());
        }
        sql.append(" ORDER BY occurred_at DESC,id DESC LIMIT ?"); args.add(query.limit() + 1);
        var rows = jdbcTemplate.query(sql.toString(), (rs, n) -> new AdminAuditEntry(
                rs.getObject("id", UUID.class), rs.getObject("actor_user_id", UUID.class),
                rs.getString("action"), rs.getString("target_type"), rs.getObject("target_id", UUID.class),
                rs.getObject("command_id", UUID.class), rs.getString("outcome"), rs.getString("reason"),
                rs.getTimestamp("occurred_at").toInstant()), args.toArray());
        boolean more = rows.size() > query.limit();
        var items = List.copyOf(rows.subList(0, Math.min(query.limit(), rows.size())));
        var last = more ? items.getLast() : null;
        return new AdminAuditPage(items, last == null ? null : new AdminAuditCursor(last.occurredAt(), last.id()).encode());
    }
}
