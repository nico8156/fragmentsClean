package com.nm.fragmentsclean.aticleContext.read;

import com.nm.fragmentsclean.aticleContext.read.projections.ArticleBlockView;
import com.nm.fragmentsclean.aticleContext.read.projections.ArticleView;
import com.nm.fragmentsclean.aticleContext.read.projections.ImageRefView;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.QueryHandler;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class GetArticleBySlugQueryHandler implements QueryHandler<GetArticleBySlugQuery, ArticleView> {

    private final JdbcTemplate jdbcTemplate;

    public GetArticleBySlugQueryHandler(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ArticleView handle(GetArticleBySlugQuery query) {

        try {
            return jdbcTemplate.queryForObject(
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
                    WHERE slug = ? AND locale = ? AND status = 'published'
                    """,
                    (rs, rowNum) -> mapRowToArticleView(rs),
                    query.slug(),
                    query.locale()
            );
        } catch (EmptyResultDataAccessException e) {
            return null; // à adapter : NotFoundException, Optionnel, etc.
        }
    }

    public ArticleView mapRowToArticleView(ResultSet rs) throws SQLException {
        UUID id = UUID.fromString(rs.getString("id"));
        String slug = rs.getString("slug");
        String locale = rs.getString("locale");

        String title = rs.getString("title");
        String intro = rs.getString("intro");
        String conclusion = rs.getString("conclusion");

        // TODO: JSON → List<ArticleBlockView>
        String blocksJson = rs.getString("blocks_json");
        List<ArticleBlockView> blocks = /* parse blocksJson */ List.of();

        String coverUrl = rs.getString("cover_url");
        Integer coverWidth = (Integer) rs.getObject("cover_width");
        Integer coverHeight = (Integer) rs.getObject("cover_height");
        String coverAlt = rs.getString("cover_alt");

        ImageRefView cover = (coverUrl != null)
                ? new ImageRefView(coverUrl, coverWidth, coverHeight, coverAlt)
                : null;

        // TODO: JSON → List<String>
        String tagsJson = rs.getString("tags_json");
        List<String> tags = /* parse tagsJson */ List.of();

        UUID authorId = UUID.fromString(rs.getString("author_id"));
        String authorName = rs.getString("author_name");

        Integer readingTimeMin = (Integer) rs.getObject("reading_time_min");
        OffsetDateTime publishedAt = rs.getObject("published_at", OffsetDateTime.class);
        OffsetDateTime updatedAt = rs.getObject("updated_at", OffsetDateTime.class);

        Long version = rs.getLong("version");
        String status = rs.getString("status");

        // TODO: JSON → List<UUID>
        String coffeeIdsJson = rs.getString("coffee_ids_json");
        List<UUID> coffeeIds = /* parse coffeeIdsJson */ List.of();

        return new ArticleView(
                id,
                slug,
                locale,
                title,
                intro,
                blocks,
                conclusion,
                cover,
                tags,
                authorId,
                authorName,
                readingTimeMin,
                publishedAt,
                updatedAt,
                version,
                status,
                coffeeIds
        );
    }
}
