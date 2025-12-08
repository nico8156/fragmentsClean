package com.nm.fragmentsclean.aticleContext.read.adapters.secondary.gateways.repositories;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleCreatedEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcArticleProjectionRepository implements ArticleProjectionRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcArticleProjectionRepository(JdbcTemplate jdbcTemplate,
                                           ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void apply(ArticleCreatedEvent event) {
        String coverJson = buildCoverJson(
                event.coverUrl(),
                event.coverWidth(),
                event.coverHeight(),
                event.coverAlt()
        );

        String tagsJson = toJson(event.tags());

        List<String> coffeeIdStrings = event.coffeeIds() != null
                ? event.coffeeIds().stream().map(UUID::toString).toList()
                : List.of();
        String coffeeIdsJson = toJson(coffeeIdStrings);

        String status = switch (event.status()) {
            case PUBLISHED -> "published";
            case DRAFT -> "draft";
            case ARCHIVED -> "archived";
        };

        jdbcTemplate.update("""
                INSERT INTO articles_projection (
                    id,
                    slug,
                    locale,
                    title,
                    intro,
                    blocks_json,
                    conclusion,
                    cover_json,
                    tags_json,
                    author_id,
                    author_name,
                    reading_time_min,
                    published_at,
                    updated_at,
                    version,
                    status,
                    coffee_ids_json
                )
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """,
                event.articleId(),
                event.slug(),
                event.locale(),
                event.title(),
                event.intro(),
                event.blocksJson(),
                event.conclusion(),
                coverJson,
                tagsJson,
                event.authorId(),
                event.authorName(),
                event.readingTimeMin(),
                Timestamp.from(event.occurredAt()),
                Timestamp.from(event.occurredAt()),
                event.version(),
                status,
                coffeeIdsJson
        );
    }

    // ─── Helpers JSON ────────────────────────────────────────────────────────

    private String buildCoverJson(String url,
                                  Integer width,
                                  Integer height,
                                  String alt) {
        if (url == null) {
            return null;
        }
        var cover = new ImageCoverDto(url, width, height, alt);
        return toJson(cover);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize JSON for articles_projection", e);
        }
    }

    private record ImageCoverDto(
            String url,
            Integer width,
            Integer height,
            String alt
    ) {}
}
