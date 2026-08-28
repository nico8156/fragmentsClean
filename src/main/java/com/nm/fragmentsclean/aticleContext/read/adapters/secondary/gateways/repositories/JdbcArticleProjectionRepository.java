package com.nm.fragmentsclean.aticleContext.read.adapters.secondary.gateways.repositories;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.aticleContext.read.projections.ArticleBlockView;
import com.nm.fragmentsclean.aticleContext.read.projections.ArticleProjectionRow;
import com.nm.fragmentsclean.aticleContext.read.projections.ImageRefView;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleCreatedEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.ArticleRevisionPublishedIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.ArticleArchivedIntegrationEvent;
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
		var source = jdbcTemplate.queryForObject("""
				SELECT a.article_id, a.slug, a.locale, a.author_id, a.author_name,
				       a.coffee_ids_json, r.title, r.introduction, r.conclusion,
				       r.cover_reference, r.cover_width, r.cover_height, r.cover_alt,
				       r.reading_time_min
				FROM articles a
				JOIN article_revisions r ON r.article_id = a.article_id
				WHERE a.article_id = ? AND r.revision_id = ?
				""", (rs, row) -> new PublishedRevisionSource(
				rs.getObject("article_id", UUID.class), rs.getString("slug"), rs.getString("locale"),
				rs.getObject("author_id", UUID.class), rs.getString("author_name"),
				rs.getString("coffee_ids_json"), rs.getString("title"), rs.getString("introduction"),
				rs.getString("conclusion"), rs.getString("cover_reference"),
				(Integer) rs.getObject("cover_width"), (Integer) rs.getObject("cover_height"),
				rs.getString("cover_alt"), rs.getInt("reading_time_min")),
				event.articleId(), event.revisionId());
		if (source == null) throw new IllegalStateException("Published article revision is missing");

		String blocksJson = toJson(jdbcTemplate.query("""
				SELECT s.section_id, s.heading,
				       COALESCE(string_agg(p.body, E'\\n\\n' ORDER BY p.position), '') AS paragraph,
				       i.storage_reference, i.width, i.height, i.alt
				FROM article_revision_sections s
				LEFT JOIN article_revision_paragraphs p ON p.section_id = s.section_id
				LEFT JOIN article_revision_images i ON i.section_id = s.section_id AND i.position = 0
				WHERE s.revision_id = ?
				GROUP BY s.section_id, s.position, s.heading, i.storage_reference, i.width, i.height, i.alt
				ORDER BY s.position
				""", (rs, row) -> new ArticleBlockView(rs.getString("heading"), rs.getString("paragraph"),
				rs.getString("storage_reference") == null ? null : new ImageRefView(
						rs.getString("storage_reference"), rs.getInt("width"), rs.getInt("height"), rs.getString("alt"))),
				event.revisionId()));
		String tagsJson = toJson(jdbcTemplate.query("""
				SELECT tag FROM article_revision_tags WHERE revision_id = ? ORDER BY position
				""", (rs, row) -> rs.getString("tag"), event.revisionId()));
		String coverJson = source.coverReference() == null ? null : toJson(new ImageCoverDto(
				source.coverReference(), source.coverWidth(), source.coverHeight(), source.coverAlt()));

		jdbcTemplate.update("""
				INSERT INTO articles_projection(id, slug, locale, title, intro, blocks_json, conclusion,
				    cover_json, tags_json, author_id, author_name, reading_time_min, published_at,
				    updated_at, version, status, coffee_ids_json)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'published', ?)
				ON CONFLICT (id) DO UPDATE SET
				    slug = EXCLUDED.slug, locale = EXCLUDED.locale, title = EXCLUDED.title,
				    intro = EXCLUDED.intro, blocks_json = EXCLUDED.blocks_json,
				    conclusion = EXCLUDED.conclusion, cover_json = EXCLUDED.cover_json,
				    tags_json = EXCLUDED.tags_json, author_id = EXCLUDED.author_id,
				    author_name = EXCLUDED.author_name, reading_time_min = EXCLUDED.reading_time_min,
				    published_at = EXCLUDED.published_at, updated_at = EXCLUDED.updated_at,
				    version = EXCLUDED.version, status = EXCLUDED.status,
				    coffee_ids_json = EXCLUDED.coffee_ids_json
				WHERE articles_projection.version < EXCLUDED.version
				""", source.articleId(), source.slug(), source.locale(), source.title(),
				source.introduction(), blocksJson, source.conclusion(), coverJson, tagsJson,
				source.authorId(), source.authorName(), source.readingTimeMin(),
				Timestamp.from(event.occurredAt()), Timestamp.from(event.occurredAt()), event.version(),
				source.coffeeIdsJson() == null ? "[]" : source.coffeeIdsJson());
	}

	private record PublishedRevisionSource(UUID articleId, String slug, String locale, UUID authorId,
			String authorName, String coffeeIdsJson, String title, String introduction, String conclusion,
			String coverReference, Integer coverWidth, Integer coverHeight, String coverAlt, int readingTimeMin) { }

	@Override
	public void apply(ArticleArchivedIntegrationEvent event) {
		jdbcTemplate.update("""
				UPDATE articles_projection SET status = 'archived', updated_at = ?, version = ?
				WHERE id = ? AND version < ?
				""", Timestamp.from(event.occurredAt()), event.version(), event.articleId(), event.version());
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
