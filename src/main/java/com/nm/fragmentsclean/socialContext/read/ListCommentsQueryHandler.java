package com.nm.fragmentsclean.socialContext.read;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;
import com.nm.fragmentsclean.socialContext.read.projections.CommentCursor;
import com.nm.fragmentsclean.socialContext.read.projections.CommentView;
import com.nm.fragmentsclean.socialContext.read.projections.CommentsListView;
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

        CommentCursor cursor = CommentCursor.parse(query.cursor());
        int pageSize = query.limit() <= 0 ? 20 : query.limit();

        // On demande une ligne de plus pour savoir s’il y a une suite
        int fetchSize = pageSize + 1;

        String baseSql = """
            SELECT *
            FROM social_comments_projection
            WHERE target_id = ?
              AND deleted_at IS NULL
            """;

        String orderBy = " ORDER BY created_at DESC, id DESC ";

        StringBuilder sql = new StringBuilder(baseSql);
        List<Object> params = new java.util.ArrayList<>();
        params.add(query.targetId());

        switch (query.op()) {
            case "older" -> {
                if (cursor != null) {
                    sql.append("""
                        AND (
                          created_at < ?
                          OR (created_at = ? AND id < ?)
                        )
                        """);
                    params.add(Timestamp.from(cursor.createdAt()));
                    params.add(Timestamp.from(cursor.createdAt()));
                    params.add(cursor.id());
                }
                // sinon, pas de filtre supplémentaire → comportement proche de "retrieve"
            }
            case "refresh" -> {
                // TODO : plus tard, pour récupérer les nouveaux commentaires
                // ex: created_at > cursor.createdAt OR (created_at = ... AND id > ...)
                // pour l'instant, tu peux le traiter comme "retrieve"
            }
            case "retrieve" -> {
                // première page → pas de condition sur le curseur
            }
            default -> throw new IllegalArgumentException("Unknown op: " + query.op());
        }

        sql.append(orderBy).append(" LIMIT ? ");
        params.add(fetchSize);

        List<CommentView> fetched = jdbcTemplate.query(
                sql.toString(),
                params.toArray(),
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

        boolean hasMore = fetched.size() > pageSize;
        List<CommentView> items = hasMore ? fetched.subList(0, pageSize) : fetched;

        String nextCursor = null;
        String prevCursor = null;

        if (!items.isEmpty()) {
            // Pour un flux descendant (createdAt DESC),
            // le "suivant" vers le passé est basé sur le dernier élément.
            CommentView last = items.get(items.size() - 1);
            nextCursor = new CommentCursor(last.createdAt(), last.id()).encode();

            // Le prevCursor (retour vers du plus récent) devient utile
            // quand tu feras navigate dans les deux sens ; pour l'instant
            // tu peux le laisser à null ou le calculer symétriquement.
            // CommentView first = items.get(0);
            // prevCursor = new CommentCursor(first.createdAt(), first.id()).encode();
        }

        return new CommentsListView(
                query.targetId(),
                query.op(),
                items,
                nextCursor,
                prevCursor,
                Instant.now()
        );
    }

    private Instant toInstantOrNull(Timestamp ts) {
        return ts != null ? Instant.ofEpochMilli(ts.getTime()) : null;
    }
}
