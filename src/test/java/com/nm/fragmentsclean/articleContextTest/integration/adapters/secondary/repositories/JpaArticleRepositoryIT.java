package com.nm.fragmentsclean.articleContextTest.integration.adapters.secondary.repositories;

import com.nm.fragmentsclean.articleContextTest.integration.AbstractJpaIntegrationTest;
import com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositorie.jpa.SpringArticleRepository;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories.ArticleRepository;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.Article;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class JpaArticleRepositoryIT extends AbstractJpaIntegrationTest {

	private static final UUID ARTICLE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private static final UUID AUTHOR_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
	private static final UUID COFFEE_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

	@Autowired
	private ArticleRepository articleRepository; // port DDD

	@Autowired
	private SpringArticleRepository springArticleRepository; // repo Spring Data (JPA)

	@Test
	void repositories_are_injected() {
		assertThat(articleRepository).isNotNull();
		assertThat(springArticleRepository).isNotNull();
	}

	@Test
	void can_save_and_reload_article_roundtrip() {
		// GIVEN
		var createdAt = Instant.parse("2024-01-01T10:00:00Z");
		var updatedAt = Instant.parse("2024-01-01T10:05:00Z");
		var publishedAt = Instant.parse("2024-01-01T10:10:00Z");

		var tags = List.of("coffee", "guide");
		var coffeeIds = List.of(COFFEE_ID);

		var blocksJson = """
				[
				  {
				    "heading": "Une sélection exigeante",
				    "paragraph": "Nous filtrons les meilleurs coffee shops.",
				    "photo": {
				      "url": "https://example.com/photo1.jpg",
				      "width": 1200,
				      "height": 800,
				      "alt": "Un latte art parfait"
				    }
				  }
				]
				""";

		var snapshot = new Article.ArticleSnapshot(
				ARTICLE_ID,
				"why-fragments-rock",
				"fr-FR",
				AUTHOR_ID,
				"Jane Doe",
				"Pourquoi Fragments va changer ta façon de boire du café",
				"Intro de l’article",
				blocksJson,
				"Conclusion de l’article",
				"https://example.com/cover.jpg",
				1600,
				900,
				"Une belle tasse de café",
				tags,
				5,
				coffeeIds,
				createdAt,
				updatedAt,
				publishedAt,
				ArticleStatus.PUBLISHED,
				1L);

		// WHEN
		articleRepository.save(Article.fromSnapshot(snapshot));

		// THEN (1) : round-trip via le port (save -> reload)
		var reloaded = articleRepository.byId(ARTICLE_ID).orElseThrow();
		var snap = reloaded.toSnapshot();

		assertThat(snap.articleId()).isEqualTo(ARTICLE_ID);
		assertThat(snap.slug()).isEqualTo("why-fragments-rock");
		assertThat(snap.locale()).isEqualTo("fr-FR");
		assertThat(snap.authorId()).isEqualTo(AUTHOR_ID);
		assertThat(snap.authorName()).isEqualTo("Jane Doe");
		assertThat(snap.title()).isEqualTo("Pourquoi Fragments va changer ta façon de boire du café");
		assertThat(snap.intro()).isEqualTo("Intro de l’article");
		assertThat(snap.blocksJson()).isEqualTo(blocksJson);
		assertThat(snap.conclusion()).isEqualTo("Conclusion de l’article");
		assertThat(snap.coverUrl()).isEqualTo("https://example.com/cover.jpg");
		assertThat(snap.coverWidth()).isEqualTo(1600);
		assertThat(snap.coverHeight()).isEqualTo(900);
		assertThat(snap.coverAlt()).isEqualTo("Une belle tasse de café");
		assertThat(snap.tags()).containsExactlyInAnyOrderElementsOf(tags);
		assertThat(snap.readingTimeMin()).isEqualTo(5);
		assertThat(snap.coffeeIds()).containsExactlyInAnyOrderElementsOf(coffeeIds);
		assertThat(snap.createdAt()).isEqualTo(createdAt);
		assertThat(snap.updatedAt()).isEqualTo(updatedAt);
		assertThat(snap.publishedAt()).isEqualTo(publishedAt);
		assertThat(snap.status()).isEqualTo(ArticleStatus.PUBLISHED);
		assertThat(snap.version()).isEqualTo(1L);

		// THEN (2) : sanity check côté JPA brut (sans égalité stricte sur JSON)
		var entities = springArticleRepository.findAll();
		assertThat(entities).hasSize(1);

		var entity = entities.getFirst();
		assertThat(entity.getArticleId()).isEqualTo(ARTICLE_ID);
		assertThat(entity.getSlug()).isEqualTo("why-fragments-rock");
		assertThat(entity.getLocale()).isEqualTo("fr-FR");
		assertThat(entity.getAuthorId()).isEqualTo(AUTHOR_ID);
		assertThat(entity.getAuthorName()).isEqualTo("Jane Doe");
		assertThat(entity.getTitle()).isEqualTo("Pourquoi Fragments va changer ta façon de boire du café");
		assertThat(entity.getBlocksJson()).isEqualTo(blocksJson);
		assertThat(entity.getStatus()).isEqualTo(ArticleStatus.PUBLISHED);

		// JSON fields: on vérifie juste que c'est bien renseigné et cohérent
		assertThat(entity.getTagsJson()).contains("coffee");
		assertThat(entity.getTagsJson()).contains("guide");
		assertThat(entity.getCoffeeIdsJson()).contains(COFFEE_ID.toString());
	}

	@Test
	void findAllPublished_returns_only_published_articles() {
		var now = Instant.parse("2024-01-01T10:00:00Z");

		var publishedSnap = new Article.ArticleSnapshot(
				UUID.fromString("11111111-1111-1111-1111-111111111111"),
				"published-article",
				"fr-FR",
				AUTHOR_ID,
				"Jane Doe",
				"Article publié",
				"Intro",
				"[]",
				"Conclusion",
				null,
				null,
				null,
				null,
				List.of("coffee"),
				3,
				List.of(),
				now,
				now,
				now,
				ArticleStatus.PUBLISHED,
				0L);

		var draftSnap = new Article.ArticleSnapshot(
				UUID.fromString("22222222-2222-2222-2222-222222222222"),
				"draft-article",
				"fr-FR",
				AUTHOR_ID,
				"Jane Doe",
				"Article draft",
				"Intro",
				"[]",
				"Conclusion",
				null,
				null,
				null,
				null,
				List.of("draft"),
				4,
				List.of(),
				now,
				now,
				null,
				ArticleStatus.DRAFT,
				0L);

		articleRepository.save(Article.fromSnapshot(publishedSnap));
		articleRepository.save(Article.fromSnapshot(draftSnap));

		// WHEN
		var publishedArticles = articleRepository.findAllPublished();

		// THEN
		assertThat(publishedArticles).hasSize(1);
		var snap = publishedArticles.getFirst().toSnapshot();

		assertThat(snap.articleId()).isEqualTo(publishedSnap.articleId());
		assertThat(snap.status()).isEqualTo(ArticleStatus.PUBLISHED);
		assertThat(snap.slug()).isEqualTo("published-article");
	}
}
