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

// mêmes imports que précédemment
public class ListCommentsQueryHandler implements QueryHandler<ListCommentsQuery, CommentsListView> {
    private final JdbcTemplate jdbcTemplate;

    public ListCommentsQueryHandler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public CommentsListView handle(ListCommentsQuery query) {

        CommentCursor cursor = CommentCursor.parse(query.cursor());
        int pageSize = query.limit() <= 0 ? 20 : query.limit();
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
            }
            case "refresh" -> {
                if (cursor != null) {
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
            }
            case "retrieve" -> { }
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
        List<CommentView> page = hasMore ? fetched.subList(0, pageSize) : fetched;

        var usersById = loadUsersPublic(page);

        List<CommentItemView> items = page.stream()
                .map(c -> {
                    var u = usersById.get(c.authorId());
                    String authorName = u != null ? u.displayName() : "Unknown";
                    String avatarUrl = u != null ? u.avatarUrl() : null;

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
                })
                .toList();

        String nextCursor = null;
        if (!items.isEmpty()) {
            CommentItemView last = items.get(items.size() - 1);
            nextCursor = new CommentCursor(last.createdAt(), last.id()).encode();
        }

        return new CommentsListView(
                query.targetId(),
                query.op(),
                items,
                nextCursor,
                null,
                Instant.now()
        );
    }

    private Map<UUID, UserRow> loadUsersPublic(List<CommentView> comments) {
        Set<UUID> ids = comments.stream()
                .map(CommentView::authorId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        if (ids.isEmpty()) return Map.of();

        String placeholders = ids.stream().map(x -> "?").collect(joining(","));
        String sql = "SELECT user_id, display_name, avatar_url FROM user_social_projection WHERE user_id IN (" + placeholders + ")";

        List<Object> params = new java.util.ArrayList<>(ids);

        List<UserRow> rows = jdbcTemplate.query(
                sql,
                params.toArray(),
                (rs, rowNum) -> new UserRow(
                        rs.getObject("user_id", UUID.class),
                        rs.getString("display_name"),
                        rs.getString("avatar_url")
                )
        );

        return rows.stream().collect(toMap(UserRow::id, r -> r));
    }

    private Instant toInstantOrNull(Timestamp ts) {
        return ts != null ? Instant.ofEpochMilli(ts.getTime()) : null;
    }

    private record UserRow(UUID id, String displayName, String avatarUrl) {}
}
