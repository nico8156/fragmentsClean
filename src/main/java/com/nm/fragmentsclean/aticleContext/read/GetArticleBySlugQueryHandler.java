package com.nm.fragmentsclean.aticleContext.read;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.nm.fragmentsclean.aticleContext.read.projections.ArticleBlockView;
import com.nm.fragmentsclean.aticleContext.read.projections.ArticleView;
import com.nm.fragmentsclean.aticleContext.read.projections.AuthorView;
import com.nm.fragmentsclean.aticleContext.read.projections.ImageRefView;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class GetArticleBySlugQueryHandler
        implements QueryHandler<GetArticleBySlugQuery, ArticleView> {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public GetArticleBySlugQueryHandler(JdbcTemplate jdbcTemplate,
                                        ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public ArticleView handle(GetArticleBySlugQuery query) {
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT *
                    FROM articles_projection
                    WHERE slug = ? AND locale = ? AND status = 'published'
                    """,
                    (rs, rowNum) -> mapRow(rs),
                    query.slug(),
                    query.locale()
            );
        } catch (EmptyResultDataAccessException e) {
            // à toi de voir : null, exception métier, etc.
            return null;
        }
    }

    ArticleView mapRowToArticleView(ResultSet rs) throws SQLException {
        return mapRow(rs);
    }

    private ArticleView mapRow(ResultSet rs) throws SQLException {
        UUID id = UUID.fromString(rs.getString("id"));
        String slug = rs.getString("slug");
        String locale = rs.getString("locale");

        String title = rs.getString("title");
        String intro = rs.getString("intro");
        String blocksJson = rs.getString("blocks_json");
        String conclusion = rs.getString("conclusion");

        String coverJson = rs.getString("cover_json");
        String tagsJson = rs.getString("tags_json");
        String coffeeIdsJson = rs.getString("coffee_ids_json");

        UUID authorId = UUID.fromString(rs.getString("author_id"));
        String authorName = rs.getString("author_name");

        int readingTimeMin = rs.getInt("reading_time_min");
        Instant publishedAt = rs.getTimestamp("published_at").toInstant();
        Instant updatedAt = rs.getTimestamp("updated_at").toInstant();

        long version = rs.getLong("version");
        String status = rs.getString("status"); // "published"

        List<ArticleBlockView> blocks = parseBlocks(blocksJson);
        ImageRefView cover = parseCover(coverJson);
        List<String> tags = parseStringList(tagsJson);
        List<UUID> coffeeIds = parseUuidList(coffeeIdsJson);

        AuthorView author = new AuthorView(authorId.toString(), authorName);

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
                author,
                readingTimeMin,
                publishedAt,
                updatedAt,
                version,
                status,
                coffeeIds
        );
    }

    // ─── Helpers JSON ────────────────────────────────────────────────────────

    private List<ArticleBlockView> parseBlocks(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            // On mappe directement sur ArticleBlockView
            return objectMapper.readValue(json, new TypeReference<List<ArticleBlockView>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse blocks_json", e);
        }
    }

    private ImageRefView parseCover(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, ImageRefView.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse cover_json", e);
        }
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse string list JSON", e);
        }
    }

    private List<UUID> parseUuidList(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            List<String> raw = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            return raw.stream().map(UUID::fromString).toList();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse UUID list JSON", e);
        }
    }
}
