package com.nm.fragmentsclean.aticleContext.read.adapters.secondary.gateways.repositories;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.aticleContext.read.projections.ArticleProjectionRow;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleCreatedEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.ArticleRevisionPublishedIntegrationEvent;
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
				event.coverAlt());

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
				ON CONFLICT (id) DO UPDATE
				SET slug = EXCLUDED.slug,
				    locale = EXCLUDED.locale,
				    title = EXCLUDED.title,
				    intro = EXCLUDED.intro,
				    blocks_json = EXCLUDED.blocks_json,
				    conclusion = EXCLUDED.conclusion,
				    cover_json = EXCLUDED.cover_json,
				    tags_json = EXCLUDED.tags_json,
				    author_id = EXCLUDED.author_id,
				    author_name = EXCLUDED.author_name,
				    reading_time_min = EXCLUDED.reading_time_min,
				    published_at = EXCLUDED.published_at,
				    updated_at = EXCLUDED.updated_at,
				    version = EXCLUDED.version,
				    status = EXCLUDED.status,
				    coffee_ids_json = EXCLUDED.coffee_ids_json
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
				coffeeIdsJson);
	}

	@Override
	public void apply(ArticleRevisionPublishedIntegrationEvent event) {
		jdbcTemplate.update("""
				UPDATE articles_projection
				SET status = 'published',
				    published_at = ?,
				    updated_at = ?,
				    version = ?
				WHERE id = ?
				  AND version < ?
				""",
				Timestamp.from(event.occurredAt()),
				Timestamp.from(event.occurredAt()),
				event.version(),
				event.articleId(),
				event.version());
	}

	@Override
	public long count() {
		Long n = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM articles_projection", Long.class);
		return n == null ? 0L : n;
	}

	@Override
	public void insertSeed(ArticleProjectionRow row) {
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
				row.id(),
				row.slug(),
				row.locale(),
				row.title(),
				row.intro(),
				row.blocksJson(),
				row.conclusion(),
				row.coverJson(),
				row.tagsJson(),
				row.authorId(),
				row.authorName(),
				row.readingTimeMin(),
				Timestamp.from(row.publishedAt()),
				Timestamp.from(row.updatedAt()),
				row.version(),
				row.status(),
				row.coffeeIdsJson());
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
			String alt) {
	}
}
