package com.nm.fragmentsclean.socialContext.read;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;
import com.nm.fragmentsclean.socialContext.read.projections.CommentCursor;
import com.nm.fragmentsclean.socialContext.read.projections.CommentItemView;
import com.nm.fragmentsclean.socialContext.read.projections.CommentView;
import com.nm.fragmentsclean.socialContext.read.projections.CommentsListView;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

public class ListCommentsQueryHandler implements QueryHandler<ListCommentsQuery, CommentsListView> {

    private static final int DEFAULT_PAGE_SIZE = 20;

    private final JdbcTemplate jdbcTemplate;

    public ListCommentsQueryHandler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public CommentsListView handle(ListCommentsQuery query) {

        CommentCursor cursor = CommentCursor.parse(query.cursor());

        int pageSize = query.limit() <= 0 ? DEFAULT_PAGE_SIZE : query.limit();
        int fetchSize = pageSize + 1;

        StringBuilder sql = new StringBuilder("""
            SELECT *
            FROM social_comments_projection
            WHERE target_id = ?
              AND deleted_at IS NULL
            """);

        List<Object> params = new ArrayList<>();
        params.add(query.targetId());

        switch (query.op()) {
            case "older" -> appendOlderClause(sql, params, cursor);
            case "refresh" -> appendRefreshClause(sql, params, cursor);
            case "retrieve" -> { /* no cursor filter */ }
            default -> throw new IllegalArgumentException("Unknown op: " + query.op());
        }

        sql.append(" ORDER BY created_at DESC, id DESC ");
        sql.append(" LIMIT ? ");
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
        List<CommentView> page = hasMore ? fetched.subList(0, pageSize) : fetched;

        Map<UUID, UserSocialRow> usersById = loadUsersSocial(page);

        List<CommentItemView> items = page.stream()
                .map(c -> toItemView(c, usersById.get(c.authorId())))
                .toList();

        String nextCursor = computeNextCursor(items, hasMore);
        String prevCursor = computePrevCursor(items);

        return new CommentsListView(
                query.targetId(),
                query.op(),
                items,
                nextCursor,
                prevCursor,
                Instant.now()
        );
    }

    private void appendOlderClause(StringBuilder sql, List<Object> params, CommentCursor cursor) {
        if (cursor == null) return;
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

    private void appendRefreshClause(StringBuilder sql, List<Object> params, CommentCursor cursor) {
        if (cursor == null) return;
        sql.append("""
            AND (
              created_at > ?
              OR (created_at = ? AND id > ?)
            )
            """);
        params.add(Timestamp.from(cursor.createdAt()));
        params.add(Timestamp.from(cursor.createdAt()));
        params.add(cursor.id());
    }

    private CommentItemView toItemView(CommentView c, UserSocialRow u) {
        String authorName = (u != null && u.displayName() != null && !u.displayName().isBlank())
                ? u.displayName()
                : "Utilisateur";

        String avatarUrl = (u != null) ? u.avatarUrl() : null;

        return new CommentItemView(
                c.id(),
                c.targetId(),
                c.parentId(),
                c.authorId(),
                authorName,
                avatarUrl,
                c.body(),
                c.createdAt(),
                c.editedAt(),
                c.likeCount(),
                c.replyCount(),
                c.version()
        );
    }

    private String computeNextCursor(List<CommentItemView> items, boolean hasMore) {
        if (!hasMore || items.isEmpty()) return null;
        CommentItemView last = items.get(items.size() - 1);
        return new CommentCursor(last.createdAt(), last.id()).encode();
    }

    private String computePrevCursor(List<CommentItemView> items) {
        if (items.isEmpty()) return null;
        CommentItemView first = items.get(0);
        return new CommentCursor(first.createdAt(), first.id()).encode();
    }

    /**
     * 2nd query: batch fetch user profiles for the current page (no JOIN).
     */
    private Map<UUID, UserSocialRow> loadUsersSocial(List<CommentView> comments) {
        List<UUID> ids = comments.stream()
                .map(CommentView::authorId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (ids.isEmpty()) return Map.of();

        String placeholders = ids.stream().map(x -> "?").collect(joining(","));

        String sql = """
            SELECT user_id, display_name, avatar_url
            FROM user_social_projection
            WHERE user_id IN (""" + placeholders + ")";

        List<UserSocialRow> rows = jdbcTemplate.query(
                sql,
                ids.toArray(),
                (rs, rowNum) -> new UserSocialRow(
                        rs.getObject("user_id", UUID.class),
                        rs.getString("display_name"),
                        rs.getString("avatar_url")
                )
        );

        return rows.stream().collect(toMap(UserSocialRow::id, r -> r));
    }

    private Instant toInstantOrNull(Timestamp ts) {
        return ts != null ? ts.toInstant() : null;
    }

    private record UserSocialRow(UUID id, String displayName, String avatarUrl) {}
}
