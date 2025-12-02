package com.nm.fragmentsclean.articleContextTest.integration.adapters.secondary.repositories;

import com.nm.fragmentsclean.articleContextTest.integration.AbstractJpaIntegrationTest;
import com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositorie.jpa.SpringArticleRepository;
import com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositorie.jpa.entities.ArticleJpaEntity;
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
    private static final UUID AUTHOR_ID  = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID COFFEE_ID  = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Autowired
    private ArticleRepository articleRepository; // port DDD

    @Autowired
    private SpringArticleRepository springArticleRepository; // repo Spring Data brut

    @Test
    void can_save_an_article() {
        var createdAt   = Instant.parse("2024-01-01T10:00:00Z");
        var updatedAt   = Instant.parse("2024-01-01T10:05:00Z");
        var publishedAt = Instant.parse("2024-01-01T10:10:00Z");

        var tags = List.of("coffee", "guide");
        var coffeeIds = List.of(COFFEE_ID);

        var snapshot = new Article.ArticleSnapshot(
                ARTICLE_ID,
                "why-fragments-rock",              // slug
                "fr-FR",                           // locale
                AUTHOR_ID,
                "Jane Doe",
                "Pourquoi Fragments va changer ta façon de boire du café",
                "Intro de l’article",
                """
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
                """,
                "Conclusion de l’article",
                "https://example.com/cover.jpg",
                1600,
                900,
                "Une belle tasse de café",
                tags,
                5,                                   // readingTimeMin
                coffeeIds,
                createdAt,
                updatedAt,
                publishedAt,
                ArticleStatus.PUBLISHED,
                1L                                   // version
        );

        // WHEN
        articleRepository.save(Article.fromSnapshot(snapshot));

        // THEN : côté JPA brut
        var entities = springArticleRepository.findAll();
        assertThat(entities).hasSize(1);
        var entity = entities.getFirst();

        // On ne vérifie pas ici le JSON exact généré par Jackson,
        // on s'assure surtout du mapping des champs simples.
        assertThat(entity).isEqualTo(
                new ArticleJpaEntity(
                        ARTICLE_ID,
                        "why-fragments-rock",
                        "fr-FR",
                        AUTHOR_ID,
                        "Jane Doe",
                        "Pourquoi Fragments va changer ta façon de boire du café",
                        "Intro de l’article",
                        snapshot.blocksJson(),      // on stocke le JSON tel quel
                        "Conclusion de l’article",
                        "https://example.com/cover.jpg",
                        1600,
                        900,
                        "Une belle tasse de café",
                        entity.getTagsJson(),       // généré par l'ObjectMapper
                        5,
                        entity.getCoffeeIdsJson(),  // idem
                        createdAt,
                        updatedAt,
                        publishedAt,
                        ArticleStatus.PUBLISHED,
                        1L
                )
        );
    }

    @Test
    void repositories_are_injected() {
        assertThat(articleRepository).isNotNull();
        assertThat(springArticleRepository).isNotNull();
    }

    @Test
    void findAllPublished_returns_only_published_articles() {
        var now = Instant.parse("2024-01-01T10:00:00Z");

        // Article publié
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
                0L
        );

        // Article en draft
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
                0L
        );

        articleRepository.save(Article.fromSnapshot(publishedSnap));
        articleRepository.save(Article.fromSnapshot(draftSnap));

        // WHEN
        var publishedArticles = articleRepository.findAllPublished();

        // THEN
        assertThat(publishedArticles)
                .hasSize(1)
                .first()
                .satisfies(article -> {
                    var snap = article.toSnapshot();
                    assertThat(snap.articleId()).isEqualTo(publishedSnap.articleId());
                    assertThat(snap.status()).isEqualTo(ArticleStatus.PUBLISHED);
                });
    }
}
