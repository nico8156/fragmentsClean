package com.nm.fragmentsclean.aticleContext.read.adapters.secondary.bootstrap;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.aticleContext.read.adapters.secondary.gateways.repositories.ArticleProjectionRepository;
import com.nm.fragmentsclean.aticleContext.read.projections.ArticleProjectionRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class ArticleReadSeedRunner implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(ArticleReadSeedRunner.class);

	private final ArticleProjectionRepository repo;
	private final ObjectMapper objectMapper;

	public ArticleReadSeedRunner(ArticleProjectionRepository repo, ObjectMapper objectMapper) {
		this.repo = repo;
		this.objectMapper = objectMapper;
	}

	@Override
	@Transactional
	public void run(String... args) throws Exception {
		seedArticlesIfEmpty();
	}

	private void seedArticlesIfEmpty() throws Exception {
		long existing = repo.count();
		if (existing > 0) {
			log.info("[SEED][articles_projection] skip (count={})", existing);
			return;
		}

		List<ArticleSeed> rows = readJsonList(
				"seed/articles.seed.json",
				new TypeReference<List<ArticleSeed>>() {
				});

		if (rows.isEmpty()) {
			log.warn("[SEED][articles_projection] no seed rows (file missing or empty)");
			return;
		}

		log.info("[SEED][articles_projection] start (items={})", rows.size());

		for (ArticleSeed r : rows) {
			UUID articleId = stableUuid("article:" + normalizeRequired(r.id(), "id"));
			UUID authorId = stableUuid("author:" + normalizeRequired(r.author().id(), "author.id"));

			String blocksJson = toJson(r.blocks());
			String coverJson = r.cover() == null ? null : toJson(r.cover());
			String tagsJson = toJson(r.tags());
			String coffeeIdsJson = toJson(
					(r.coffeeIds() == null ? List.<UUID>of() : r.coffeeIds())
							.stream()
							.map(UUID::toString)
							.toList());

			ArticleProjectionRow row = new ArticleProjectionRow(
					articleId,
					normalizeRequired(r.slug(), "slug"),
					normalizeRequired(r.locale(), "locale"),
					normalizeRequired(r.title(), "title"),
					normalizeRequired(r.intro(), "intro"),
					normalizeRequired(blocksJson, "blocks_json"),
					normalizeRequired(r.conclusion(), "conclusion"),
					coverJson,
					normalizeRequired(tagsJson, "tags_json"),
					authorId,
					normalizeRequired(r.author().name(), "author.name"),
					r.readingTimeMin(),
					r.publishedAt(),
					r.updatedAt(),
					r.version(),
					normalizeRequired(r.status(), "status"),
					normalizeRequired(coffeeIdsJson, "coffee_ids_json"));

			repo.insertSeed(row);
		}

		log.info("[SEED][articles_projection] done");
	}

	private <T> List<T> readJsonList(String classpathLocation, TypeReference<List<T>> ref) throws Exception {
		ClassPathResource resource = new ClassPathResource(classpathLocation);
		if (!resource.exists())
			return List.of();

		try (InputStream in = resource.getInputStream()) {
			return objectMapper.readValue(in, ref);
		}
	}

	private String toJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to serialize seed JSON for articles_projection", e);
		}
	}

	private static UUID stableUuid(String key) {
		return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
	}

	private static String normalizeRequired(String s, String fieldName) {
		if (s == null)
			throw new IllegalArgumentException("Missing required field: " + fieldName);
		String t = s.trim();
		if (t.isEmpty())
			throw new IllegalArgumentException("Empty required field: " + fieldName);
		return t;
	}

	// ──────────────────────────────────────────────────────────────────────
	// Seed DTOs (contract == ArticleView)
	// ──────────────────────────────────────────────────────────────────────

	public record ArticleSeed(
			String id,
			String slug,
			String locale,

			String title,
			String intro,
			List<ArticleBlockView> blocks,
			String conclusion,

			ImageRefView cover,
			List<String> tags,
			AuthorView author,

			int readingTimeMin,
			Instant publishedAt,
			Instant updatedAt,

			long version,
			String status,

			List<UUID> coffeeIds) {
	}

	public record ArticleBlockView(
			String heading,
			String paragraph,
			ImageRefView photo) {
	}

	public record ImageRefView(
			String url,
			Integer width,
			Integer height,
			String alt) {
	}

	public record AuthorView(
			String id,
			String name) {
	}
}
