package com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.security;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.AdminUserAccess;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.AdminUserAccessRepository;

@Repository
public class JdbcAdminUserAccessRepository implements AdminUserAccessRepository {
	private final JdbcTemplate jdbcTemplate;

	public JdbcAdminUserAccessRepository(JdbcTemplate jdbcTemplate) { this.jdbcTemplate = jdbcTemplate; }

	@Override
	public List<AdminUserAccess> list() {
		return jdbcTemplate.query("""
				SELECT a.user_id, u.email, a.granted_at, a.granted_by
				FROM admin_user_access a JOIN auth_users u ON u.id = a.user_id
				ORDER BY a.granted_at ASC
				""", this::mapRow);
	}

	@Override
	public Optional<AdminUserAccess> findByUserId(UUID userId) {
		return jdbcTemplate.query("""
				SELECT a.user_id, u.email, a.granted_at, a.granted_by
				FROM admin_user_access a JOIN auth_users u ON u.id = a.user_id
				WHERE a.user_id = ?
				""", this::mapRow, userId).stream().findFirst();
	}

	@Override
	public Optional<UUID> findAuthUserIdByEmail(String email) {
		return jdbcTemplate.query("SELECT id FROM auth_users WHERE lower(email) = lower(?)",
				(rs, rowNum) -> rs.getObject("id", UUID.class), email).stream().findFirst();
	}

	@Override
	public boolean isAllowed(UUID userId, String email) {
		return jdbcTemplate.queryForObject("""
				SELECT EXISTS(
					SELECT 1 FROM admin_user_access a JOIN auth_users u ON u.id = a.user_id
					WHERE a.user_id = ? OR lower(u.email) = lower(?)
				)
				""", Boolean.class, userId, email == null ? "" : email);
	}

	@Override
	public void grant(UUID userId, UUID grantedBy, Instant grantedAt) {
		jdbcTemplate.update("""
				INSERT INTO admin_user_access (user_id, granted_at, granted_by)
				VALUES (?, ?, ?) ON CONFLICT (user_id) DO NOTHING
				""", userId, Timestamp.from(grantedAt), grantedBy);
	}

	@Override
	public void revoke(UUID userId) {
		jdbcTemplate.update("DELETE FROM admin_user_access WHERE user_id = ?", userId);
	}

	@Override
	public long count() { return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM admin_user_access", Long.class); }

	private AdminUserAccess mapRow(ResultSet rs, int rowNum) throws SQLException {
		return new AdminUserAccess(rs.getObject("user_id", UUID.class), rs.getString("email"),
				rs.getTimestamp("granted_at").toInstant(), rs.getObject("granted_by", UUID.class), false);
	}
}
