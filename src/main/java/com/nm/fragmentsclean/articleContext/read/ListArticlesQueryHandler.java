package com.nm.fragmentsclean.articleContext.read;

import com.nm.fragmentsclean.articleContext.read.projections.ArticleCursor;
import com.nm.fragmentsclean.articleContext.read.projections.ArticleListView;
import com.nm.fragmentsclean.articleContext.read.projections.ArticleView;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

public class ListArticlesQueryHandler implements QueryHandler<ListArticlesQuery, ArticleListView> {
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    private final JdbcTemplate jdbcTemplate;
    private final GetArticleBySlugQueryHandler articleRowMapper;

    public ListArticlesQueryHandler(JdbcTemplate jdbcTemplate, GetArticleBySlugQueryHandler articleRowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.articleRowMapper = articleRowMapper;
    }

    @Override
    public ArticleListView handle(ListArticlesQuery query) {
        int limit = normalizeLimit(query.limit());
        ArticleCursor cursor = ArticleCursor.decode(query.cursor()).orElse(null);
        boolean previous = cursor != null && cursor.direction() == ArticleCursor.Direction.PREVIOUS;
        StringBuilder sql = new StringBuilder("""
                SELECT id, slug, locale, title, intro, blocks_json, conclusion,
                       cover_json, tags_json, author_id, author_name,
                       reading_time_min, published_at, updated_at, version,
                       status, coffee_ids_json
                FROM articles_projection
                WHERE locale = ? AND status = 'published'
                """);
        List<Object> parameters = new ArrayList<>();
        parameters.add(query.locale());

        if (cursor != null) {
            String comparison = previous ? ">" : "<";
            sql.append(" AND (published_at ").append(comparison)
                    .append(" ? OR (published_at = ? AND id ").append(comparison).append(" ?))");
            parameters.add(Timestamp.from(cursor.publishedAt()));
            parameters.add(Timestamp.from(cursor.publishedAt()));
            parameters.add(cursor.articleId());
        }
        sql.append(previous
                ? " ORDER BY published_at ASC, id ASC LIMIT ?"
                : " ORDER BY published_at DESC, id DESC LIMIT ?");
        parameters.add(limit + 1);

        List<ArticleView> items = new ArrayList<>(jdbcTemplate.query(
                sql.toString(), (rs, rowNum) -> articleRowMapper.mapRowToArticleView(rs), parameters.toArray()));
        boolean hasMore = items.size() > limit;
        if (hasMore) items.remove(items.size() - 1);
        if (previous) Collections.reverse(items);

        return new ArticleListView(
                List.copyOf(items),
                nextCursor(items, cursor, hasMore),
                previousCursor(items, cursor, hasMore),
                listEtag(query.locale(), items));
    }

    private int normalizeLimit(Integer requested) {
        return requested == null ? DEFAULT_LIMIT : Math.max(1, Math.min(requested, MAX_LIMIT));
    }

    private String nextCursor(List<ArticleView> items, ArticleCursor cursor, boolean hasMore) {
        if (items.isEmpty() || (!hasMore && (cursor == null || cursor.direction() != ArticleCursor.Direction.PREVIOUS))) {
            return null;
        }
        ArticleView last = items.get(items.size() - 1);
        return ArticleCursor.next(last.publishedAt(), last.id()).encode();
    }

    private String previousCursor(List<ArticleView> items, ArticleCursor cursor, boolean hasMore) {
        if (items.isEmpty() || cursor == null
                || (cursor.direction() == ArticleCursor.Direction.PREVIOUS && !hasMore)) {
            return null;
        }
        ArticleView first = items.get(0);
        return ArticleCursor.previous(first.publishedAt(), first.id()).encode();
    }

    private String listEtag(String locale, List<ArticleView> items) {
        long fingerprint = items.stream()
                .mapToLong(item -> 31L * item.version() + item.id().hashCode())
                .reduce(1L, (left, right) -> 31L * left + right);
        return "articles-" + locale + '-' + Long.toUnsignedString(fingerprint, 36);
    }
}
