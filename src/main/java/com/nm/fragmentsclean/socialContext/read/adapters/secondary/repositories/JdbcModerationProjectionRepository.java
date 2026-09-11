package com.nm.fragmentsclean.socialContext.read.adapters.secondary.repositories;

import com.nm.fragmentsclean.socialContext.write.businesslogic.models.*;
import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcModerationProjectionRepository {
    private final JdbcTemplate jdbc;
    public JdbcModerationProjectionRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void apply(CommentReportedEvent e) {
        jdbc.update("""
            INSERT INTO social_content_reports_projection
              (report_id, comment_id, target_id, author_id, reporter_id, reason, details, status, created_at, resolved_at, version)
            VALUES (?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT (report_id) DO NOTHING
            """, e.reportId(), e.commentId(), e.targetId(), e.authorId(), e.reporterId(), e.reason().name(),
                e.details(), e.status().name(), Timestamp.from(e.occurredAt()), null, e.version());
    }

    public void apply(UserBlockChangedEvent e) {
        jdbc.update("""
            INSERT INTO social_user_blocks_projection
              (block_id, blocker_id, blocked_user_id, active, updated_at, version)
            VALUES (?,?,?,?,?,?)
            ON CONFLICT (block_id) DO UPDATE SET active=EXCLUDED.active, updated_at=EXCLUDED.updated_at,
              version=EXCLUDED.version WHERE social_user_blocks_projection.version < EXCLUDED.version
            """, e.blockId(), e.blockerId(), e.blockedUserId(), e.active(), Timestamp.from(e.occurredAt()), e.version());
    }

    public void apply(CommentModeratedEvent e) {
        jdbc.update("""
            UPDATE social_comments_projection SET moderation=?, version=?
            WHERE id=? AND version < ?
            """, e.moderation().name(), e.version(), e.commentId(), e.version());
        jdbc.update("""
            UPDATE social_content_reports_projection SET status=?, resolved_at=?, version=version+1
            WHERE comment_id=? AND status='OPEN'
            """, e.reportStatus().name(), Timestamp.from(e.occurredAt()), e.commentId());
        jdbc.update("""
            UPDATE social_content_reports_projection SET status=?, resolved_at=?, version=version+1
            WHERE report_id=? AND status<>?
            """, e.reportStatus().name(), Timestamp.from(e.occurredAt()), e.reportId(), e.reportStatus().name());
        jdbc.update("""
            INSERT INTO social_moderation_actions_projection
              (action_id, report_id, comment_id, operator_id, decision, reason, occurred_at)
            VALUES (?,?,?,?,?,?,?) ON CONFLICT (action_id) DO NOTHING
            """, e.actionId(), e.reportId(), e.commentId(), e.operatorId(), e.moderation().name(),
                e.reason(), Timestamp.from(e.occurredAt()));
    }
}
