package com.nm.fragmentsclean.aticleContext.read.projections;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleCreatedEvent;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;

import java.util.UUID;

@Component
public class ArticleProjectionHandler {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ArticleProjectionHandler(JdbcTemplate jdbcTemplate,
                                    ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void on(ArticleCreatedEvent event) {
        String coverJson = toCoverJson(
                event.coverUrl(),
                event.coverWidth(),
                event.coverHeight(),
                event.coverAlt()
        );

        String tagsJson = toJson(event.tags());
        String coffeeIdsJson = toJson(
                event.coffeeIds().stream().map(UUID::toString).toList()
        );

        // on utilise occurredAt comme "server time"
        var ts = Timestamp.from(event.occurredAt());

        // 1) tentative d'UPDATE (idempotence + replays)
        int updated = jdbcTemplate.update("""
                UPDATE articles_projection
                SET slug = ?,
                    locale = ?,
                    title = ?,
                    intro = ?,
                    blocks_json = ?,
                    conclusion = ?,
                    cover_json = ?,
                    tags_json = ?,
                    author_id = ?,
                    author_name = ?,
                    reading_time_min = ?,
                    published_at = ?,
                    updated_at = ?,
                    version = ?,
                    status = ?,
                    coffee_ids_json = ?
                WHERE id = ?
                """,
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
                ts,
                ts,
                event.version(),
                event.status().name().toLowerCase(), // "published"
                coffeeIdsJson,
                event.articleId()
        );

        if (updated == 0) {
            // 2) sinon on INSERT
            jdbcTemplate.update("""
                    INSERT INTO articles_projection(
                        id,
                        slug, locale,
                        title, intro, blocks_json, conclusion,
                        cover_json, tags_json,
                        author_id, author_name,
                        reading_time_min,
                        published_at, updated_at,
                        version, status,
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
                    ts,
                    ts,
                    event.version(),
                    event.status().name().toLowerCase(),
                    coffeeIdsJson
            );
        }
    }

    // ─── Helpers JSON ────────────────────────────────────────────────────────

    private String toCoverJson(String url,
                               Integer width,
                               Integer height,
                               String alt) {
        if (url == null) {
            return null;
        }

        var cover = new ImageRefProjection(
                url,
                width != null ? width : 0,
                height != null ? height : 0,
                alt
        );

        return toJson(cover);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize JSON in ArticleProjectionHandler", e);
        }
    }

    private record ImageRefProjection(
            String url,
            int width,
            int height,
            String alt
    ) {}
}
