package com.nm.fragmentsclean.socialContext.read;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.QueryHandler;
import com.nm.fragmentsclean.socialContext.read.projections.CommentView;
import org.springframework.jdbc.core.JdbcTemplate;
import java.sql.Timestamp;
import java.time.Instant;

import java.util.List;
import java.util.UUID;

public class ListCommentsQueryHandler implements QueryHandler<ListCommentsQuery, CommentsListView> {
    private final JdbcTemplate jdbcTemplate;

    public ListCommentsQueryHandler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public CommentsListView handle(ListCommentsQuery query) {

        // TODO: adapte le nom de table / colonnes à ta projection réelle
        String sql = """
            SELECT *
            FROM social_comments_projection
            WHERE target_id = ?
              AND (deleted_at IS NULL)
            ORDER BY created_at DESC
            LIMIT ?
            """;

        List<CommentView> items = jdbcTemplate.query(
                sql,
                new Object[]{query.targetId(), query.limit()},
                (rs, rowNum) -> new CommentView(
                        rs.getObject("id", UUID.class),
                        rs.getObject("target_id", UUID.class),
                        rs.getObject("parent_id", UUID.class),
                        rs.getObject("author_id", UUID.class),
                        rs.getString("body"),
                        rs.getTimestamp("created_at").toInstant(),
                        toInstantOrNull(rs.getTimestamp("edited_at")),
                        toInstantOrNull(rs.getTimestamp("deleted_at")),
                        rs.getString("moderation"),
                        rs.getLong("like_count"),
                        rs.getLong("reply_count"),
                        rs.getLong("version")
                )
        );

        return new CommentsListView(
                query.targetId(),
                query.op(),
                items,
                null,                   // nextCursor (à implémenter plus tard)
                null,                   // prevCursor
                Instant.now()           // ou bien un DateTimeProvider
        );
    }

    private Instant toInstantOrNull(Timestamp ts) {
        return ts != null ? Instant.ofEpochMilli(ts.getTime()) : null;
    }
}
