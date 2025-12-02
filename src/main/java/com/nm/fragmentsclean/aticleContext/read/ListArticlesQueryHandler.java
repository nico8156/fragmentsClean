package com.nm.fragmentsclean.aticleContext.read;

import com.nm.fragmentsclean.aticleContext.read.projections.ArticleListView;
import com.nm.fragmentsclean.aticleContext.read.projections.ArticleView;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

public class ListArticlesQueryHandler implements QueryHandler<ListArticlesQuery, ArticleListView> {

    private static final int DEFAULT_LIMIT = 10;

    private final JdbcTemplate jdbcTemplate;
    private final GetArticleBySlugQueryHandler getArticleBySlugQueryHandler;


    public ListArticlesQueryHandler(JdbcTemplate jdbcTemplate, GetArticleBySlugQueryHandler getArticleBySlugQueryHandler) {
        this.jdbcTemplate = jdbcTemplate;
        this.getArticleBySlugQueryHandler = getArticleBySlugQueryHandler;
    }

    @Override
    public ArticleListView handle(ListArticlesQuery query) {
        int limit = query.limit() != null ? query.limit() : DEFAULT_LIMIT;

        // TODO: décoder le cursor pour retrouver la position (offset, lastId, etc.)
        int offset = decodeCursorToOffset(query.cursor());

        List<ArticleView> items = jdbcTemplate.query(
                """
                SELECT
                    id,
                    slug,
                    locale,
                    title,
                    intro,
                    blocks_json,
                    conclusion,
                    cover_url,
                    cover_width,
                    cover_height,
                    cover_alt,
                    tags_json,
                    author_id,
                    author_name,
                    reading_time_min,
                    published_at,
                    updated_at,
                    version,
                    status,
                    coffee_ids_json
                FROM articles_projection
                WHERE locale = ? AND status = 'published'
                ORDER BY published_at DESC
                LIMIT ? OFFSET ?
                """,
                (rs, rowNum) ->  getArticleBySlugQueryHandler.mapRowToArticleView(rs),
                query.locale(),
                limit,
                offset
        );

        String nextCursor = items.size() == limit
                ? encodeOffsetToCursor(offset + limit)
                : null;
        String prevCursor = offset > 0
                ? encodeOffsetToCursor(Math.max(offset - limit, 0))
                : null;

        // ETag de liste : simpliste → basé sur locale + offset
        String etag = "articles-" + query.locale() + "-offset-" + offset;

        return new ArticleListView(
                items,
                nextCursor,
                prevCursor,
                etag
        );
    }

    private int decodeCursorToOffset(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(cursor);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String encodeOffsetToCursor(int offset) {
        return String.valueOf(offset);
    }
}
