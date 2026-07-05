package com.nm.fragmentsclean.socialContext.read.adapters.secondary.repositories;

import com.nm.fragmentsclean.socialContext.read.projections.LikeStatusView;
import com.nm.fragmentsclean.socialContext.read.projections.LikeSummaryView;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.LikeSetEvent;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcLikeProjectionRepository {
	private final JdbcTemplate jdbcTemplate;

	public JdbcLikeProjectionRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void apply(LikeSetEvent event) {
		jdbcTemplate.update("""
				INSERT INTO social_likes_projection (
				  like_id, user_id, target_id, active, version, updated_at
				)
				VALUES (?, ?, ?, ?, ?, ?)
				ON CONFLICT (like_id) DO UPDATE SET
				  user_id = EXCLUDED.user_id,
				  target_id = EXCLUDED.target_id,
				  active = EXCLUDED.active,
				  version = EXCLUDED.version,
				  updated_at = EXCLUDED.updated_at
				WHERE social_likes_projection.version <= EXCLUDED.version
				""",
				event.likeId(),
				event.userId(),
				event.targetId(),
				event.active(),
				event.version(),
				ts(event.occurredAt()));
	}

	public LikeStatusView statusFor(UUID targetId, UUID currentUserId, Instant serverTime) {
		long count = countActive(targetId);
		boolean me = isActiveForUser(targetId, currentUserId);
		long version = maxVersion(targetId);
		return new LikeStatusView(count, me, version, serverTime.toString());
	}

	public LikeSummaryView summaryFor(UUID userId, UUID targetId) {
		return new LikeSummaryView(userId, targetId, isActiveForUser(targetId, userId), countActive(targetId));
	}

	private long countActive(UUID targetId) {
		Long count = jdbcTemplate.queryForObject(
				"""
				SELECT COUNT(*)
				FROM social_likes_projection
				WHERE target_id = ? AND active = true
				""",
				Long.class,
				targetId);
		return count == null ? 0L : count;
	}

	private boolean isActiveForUser(UUID targetId, UUID userId) {
		Boolean active = jdbcTemplate.query(
				"""
				SELECT active
				FROM social_likes_projection
				WHERE target_id = ? AND user_id = ?
				""",
				rs -> rs.next() && rs.getBoolean("active"),
				targetId,
				userId);
		return Boolean.TRUE.equals(active);
	}

	private long maxVersion(UUID targetId) {
		Long version = jdbcTemplate.queryForObject(
				"""
				SELECT COALESCE(MAX(version), 0)
				FROM social_likes_projection
				WHERE target_id = ?
				""",
				Long.class,
				targetId);
		return version == null ? 0L : version;
	}

	private Timestamp ts(Instant instant) {
		return instant == null ? null : Timestamp.from(instant);
	}
}
