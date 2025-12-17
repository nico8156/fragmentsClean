package com.nm.fragmentsclean.socialContext.read.adapters.secondary.repositories;

import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentCreatedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentDeletedEvent;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.CommentUpdatedEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

@Repository
public class JdbcCommentProjectionRepository implements SocialCommentsProjectionRepository{

    private final JdbcTemplate jdbcTemplate;

    public JdbcCommentProjectionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void apply(CommentCreatedEvent e) {
        jdbcTemplate.update("""
        INSERT INTO social_comments_projection (
          id, target_id, author_id, parent_id,
          body, created_at, edited_at, deleted_at,
          moderation, like_count, reply_count, version
        )
        VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
        ON CONFLICT (id) DO NOTHING
        """,
                e.commentId(),
                e.targetId(),
                e.authorId(),
                e.parentId(),
                e.body(),
                Timestamp.from(e.occurredAt()),     // ✅ ici
                null,
                null,
                e.moderation().name(),              // ou String.valueOf(e.moderation())
                0L,
                0L,
                e.version()
        );

        if (e.parentId() != null) {
            jdbcTemplate.update("""
            UPDATE social_comments_projection
            SET reply_count = reply_count + 1
            WHERE id = ?
            """, e.parentId()
            );
        }
    }

    @Override
    public void apply(CommentUpdatedEvent e) {
        jdbcTemplate.update("""
        UPDATE social_comments_projection
        SET body = ?,
            edited_at = ?,
            moderation = ?,
            version = ?
        WHERE id = ?
          AND target_id = ?
          AND version < ?
        """,
                e.body(),
                Timestamp.from(e.occurredAt()),   // edited_at = timestamp serveur (cohérent)
                e.moderation().name(),
                e.version(),
                e.commentId(),
                e.targetId(),
                e.version()
        );
    }

    @Override
    public void apply(CommentDeletedEvent e) {
        jdbcTemplate.update("""
        UPDATE social_comments_projection
        SET deleted_at = ?,
            moderation = ?,
            version = ?
        WHERE id = ?
          AND target_id = ?
          AND version < ?
        """,
                Timestamp.from(e.deletedAt()),    // ✅ utilise deletedAt (plus sémantique)
                e.moderation().name(),
                e.version(),
                e.commentId(),
                e.targetId(),
                e.version()
        );
    }

    private String toModerationString(CommentCreatedEvent e) {
        // adapte selon ton type: String / enum
        return String.valueOf(e.moderation());
    }

    private String toModerationString(CommentUpdatedEvent e) {
        return String.valueOf(e.moderation());
    }

    private String toModerationString(CommentDeletedEvent e) {
        return String.valueOf(e.moderation());
    }
}
