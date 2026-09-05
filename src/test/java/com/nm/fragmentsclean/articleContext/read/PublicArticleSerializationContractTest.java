package com.nm.fragmentsclean.articleContext.read;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.nm.fragmentsclean.articleContext.read.projections.ArticleView;
import com.nm.fragmentsclean.articleContext.read.projections.AuthorView;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.configuration.SharedKernelDependenciesConfiguration;

class PublicArticleSerializationContractTest {
	@Test
	void exposes_article_instants_as_iso_8601_strings() throws Exception {
		Instant publishedAt = Instant.parse("2026-08-28T17:31:53.136Z");
		Instant updatedAt = Instant.parse("2026-08-28T17:32:04.250Z");
		var article = new ArticleView(
				UUID.fromString("11111111-1111-1111-1111-111111111111"),
				"guide-cafe",
				"fr-FR",
				"Guide café",
				"Introduction",
				List.of(),
				"Conclusion",
				null,
				List.of("decouverte"),
				new AuthorView("22222222-2222-2222-2222-222222222222", "Fragments Studio"),
				3,
				publishedAt,
				updatedAt,
				1,
				"published",
				List.of());

		String json = new SharedKernelDependenciesConfiguration().objectMapper().writeValueAsString(article);

		assertThat(json).contains("\"publishedAt\":\"2026-08-28T17:31:53.136Z\"");
		assertThat(json).contains("\"updatedAt\":\"2026-08-28T17:32:04.250Z\"");
		assertThat(json).doesNotContain("1787938313.136");
	}
}
