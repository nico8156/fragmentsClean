package com.nm.fragmentsclean.articleContextTest.integration.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.TestContainers;
import com.nm.fragmentsclean.aticleContext.read.adapters.secondary.bootstrap.ArticleReadSeedRunner;
import com.nm.fragmentsclean.aticleContext.read.adapters.secondary.gateways.repositories.ArticleProjectionRepository;

@SpringBootTest
@ActiveProfiles("database")
class ArticleReadSeedRunnerIT extends TestContainers {

	@Autowired
	ArticleReadSeedRunner runner;

	@Autowired
	ArticleProjectionRepository repo;

	@Autowired
	JdbcTemplate jdbcTemplate;

	@Autowired
	ObjectMapper objectMapper;

	@BeforeEach
	void resetDb() {
		jdbcTemplate.execute("TRUNCATE TABLE articles_projection");
	}

	@Test
	void seed_populates_exactly_seed_file_size() throws Exception {
		// arrange
		int expected = seedArraySize("seed/articles.seed.json");

		// act
		runner.run();

		// assert
		assertThat(repo.count()).isEqualTo(expected);

		// sanity: un article connu seedé (slug)
		String slug = jdbcTemplate.queryForObject(
				"SELECT slug FROM articles_projection WHERE slug = ?",
				String.class,
				"quest-ce-que-le-cafe-de-specialite");
		assertThat(slug).isEqualTo("quest-ce-que-le-cafe-de-specialite");

		// sanity: locale ok
		String locale = jdbcTemplate.queryForObject(
				"SELECT locale FROM articles_projection WHERE slug = ?",
				String.class,
				"quest-ce-que-le-cafe-de-specialite");
		assertThat(locale).isEqualTo("fr-FR");

		// sanity: blocks_json est un tableau JSON non vide pour l'article 001
		String blocksJson = jdbcTemplate.queryForObject(
				"SELECT blocks_json FROM articles_projection WHERE slug = ?",
				String.class,
				"quest-ce-que-le-cafe-de-specialite");
		assertThat(blocksJson).isNotBlank();
		JsonNode blocks = objectMapper.readTree(blocksJson);
		assertThat(blocks.isArray()).isTrue();
		assertThat(blocks.size()).isGreaterThan(0);

		// sanity: tags_json est un tableau JSON
		String tagsJson = jdbcTemplate.queryForObject(
				"SELECT tags_json FROM articles_projection WHERE slug = ?",
				String.class,
				"quest-ce-que-le-cafe-de-specialite");
		JsonNode tags = objectMapper.readTree(tagsJson);
		assertThat(tags.isArray()).isTrue();

		// sanity: coffee_ids_json est un tableau JSON (souvent vide)
		String coffeeIdsJson = jdbcTemplate.queryForObject(
				"SELECT coffee_ids_json FROM articles_projection WHERE slug = ?",
				String.class,
				"quest-ce-que-le-cafe-de-specialite");
		JsonNode coffeeIds = objectMapper.readTree(coffeeIdsJson);
		assertThat(coffeeIds.isArray()).isTrue();
	}

	@Test
	void seed_is_idempotent() throws Exception {
		runner.run();
		long n1 = repo.count();

		runner.run();
		long n2 = repo.count();

		assertThat(n2).isEqualTo(n1);
	}

	private int seedArraySize(String classpathLocation) throws Exception {
		ClassPathResource resource = new ClassPathResource(classpathLocation);
		try (InputStream in = resource.getInputStream()) {
			JsonNode root = objectMapper.readTree(in);
			assertThat(root.isArray())
					.as("Seed file must be a JSON array: " + classpathLocation)
					.isTrue();
			return root.size();
		}
	}
}
