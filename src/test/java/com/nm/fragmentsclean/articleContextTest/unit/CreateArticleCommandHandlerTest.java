package com.nm.fragmentsclean.articleContextTest.unit;

import com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositorie.fake.FakeArticleRepository;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleCreatedEvent;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleStatus;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article.CreateArticleCommand;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article.CreateArticleCommandHandler;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventPublisher.FakeDomainEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateArticleCommandHandlerTest {

    private final UUID ARTICLE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private final UUID AUTHOR_ID  = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private final UUID CMD_ID     = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private final UUID COFFEE_ID  = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    FakeArticleRepository articleRepository = new FakeArticleRepository();
    FakeDomainEventPublisher domainEventPublisher = new FakeDomainEventPublisher();
    DeterministicDateTimeProvider dateTimeProvider = new DeterministicDateTimeProvider();

    CreateArticleCommandHandler handler;

    @BeforeEach
    void setup() {
        dateTimeProvider.instantOfNow = Instant.parse("2023-10-01T10:00:00Z");
        handler = new CreateArticleCommandHandler(articleRepository, domainEventPublisher, dateTimeProvider);
    }

    @Test
    void should_create_new_article_and_publish_event() {
        // GIVEN
        String slug = "why-fragments-rock";
        String locale = "fr-FR";
        String title = "Pourquoi Fragments va changer ta façon de boire du café";
        String intro = "Dans cet article, on va explorer comment Fragments t'aide à trouver des cafés incroyables.";
        String blocksJson = """
            [
              {
                "heading": "Une sélection exigeante",
                "paragraph": "Nous filtrons pour toi les cafés vraiment passionnés.",
                "photo": {
                  "url": "https://example.com/photo1.jpg",
                  "width": 1200,
                  "height": 800,
                  "alt": "Un latte art parfait"
                }
              },
              {
                "heading": "Une expérience locale",
                "paragraph": "Découvre les coffee shops indépendants autour de toi."
              }
            ]
            """;
        String conclusion = "En bref : moins de scroll, plus de bons cafés.";
        String coverUrl = "https://example.com/cover.jpg";
        int coverWidth = 1600;
        int coverHeight = 900;
        String coverAlt = "Une tasse de café vue de dessus";
        List<String> tags = List.of("coffee", "rennes", "guide");
        int readingTimeMin = 5;
        List<UUID> coffeeIds = List.of(COFFEE_ID);

        Instant clientAt = Instant.parse("2023-10-01T09:59:00Z");

        // WHEN
        handler.execute(new CreateArticleCommand(
                CMD_ID,
                clientAt,
                ARTICLE_ID,
                slug,
                locale,
                AUTHOR_ID,
                "Jane Doe",
                title,
                intro,
                blocksJson,
                conclusion,
                coverUrl,
                coverWidth,
                coverHeight,
                coverAlt,
                tags,
                readingTimeMin,
                coffeeIds
        ));

        // THEN : état en mémoire (snapshot)
        var snaps = articleRepository.allSnapshots();
        assertThat(snaps).hasSize(1);
        var snap = snaps.getFirst();

        assertThat(snap.articleId()).isEqualTo(ARTICLE_ID);
        assertThat(snap.slug()).isEqualTo(slug);
        assertThat(snap.locale()).isEqualTo(locale);
        assertThat(snap.authorId()).isEqualTo(AUTHOR_ID);
        assertThat(snap.authorName()).isEqualTo("Jane Doe");

        assertThat(snap.title()).isEqualTo(title);
        assertThat(snap.intro()).isEqualTo(intro);
        assertThat(snap.blocksJson()).isEqualTo(blocksJson);
        assertThat(snap.conclusion()).isEqualTo(conclusion);

        assertThat(snap.coverUrl()).isEqualTo(coverUrl);
        assertThat(snap.coverWidth()).isEqualTo(coverWidth);
        assertThat(snap.coverHeight()).isEqualTo(coverHeight);
        assertThat(snap.coverAlt()).isEqualTo(coverAlt);

        assertThat(snap.tags()).containsExactlyElementsOf(tags);
        assertThat(snap.readingTimeMin()).isEqualTo(readingTimeMin);
        assertThat(snap.coffeeIds()).containsExactly(COFFEE_ID);

        assertThat(snap.createdAt()).isEqualTo(dateTimeProvider.instantOfNow);
        assertThat(snap.updatedAt()).isEqualTo(dateTimeProvider.instantOfNow);
        assertThat(snap.publishedAt()).isEqualTo(dateTimeProvider.instantOfNow);
        assertThat(snap.status()).isEqualTo(ArticleStatus.PUBLISHED);
        assertThat(snap.version()).isEqualTo(0L); // première version, pas d’update encore

        // THEN : event publié
        assertThat(domainEventPublisher.published).hasSize(1);
        var evt = (ArticleCreatedEvent) domainEventPublisher.published.getFirst();

        assertThat(evt.commandId()).isEqualTo(CMD_ID);
        assertThat(evt.articleId()).isEqualTo(ARTICLE_ID);
        assertThat(evt.slug()).isEqualTo(slug);
        assertThat(evt.locale()).isEqualTo(locale);
        assertThat(evt.authorId()).isEqualTo(AUTHOR_ID);
        assertThat(evt.authorName()).isEqualTo("Jane Doe");

        assertThat(evt.title()).isEqualTo(title);
        assertThat(evt.intro()).isEqualTo(intro);
        assertThat(evt.blocksJson()).isEqualTo(blocksJson);
        assertThat(evt.conclusion()).isEqualTo(conclusion);

        assertThat(evt.coverUrl()).isEqualTo(coverUrl);
        assertThat(evt.coverWidth()).isEqualTo(coverWidth);
        assertThat(evt.coverHeight()).isEqualTo(coverHeight);
        assertThat(evt.coverAlt()).isEqualTo(coverAlt);

        assertThat(evt.tags()).containsExactlyElementsOf(tags);
        assertThat(evt.readingTimeMin()).isEqualTo(readingTimeMin);
        assertThat(evt.coffeeIds()).containsExactly(COFFEE_ID);

        assertThat(evt.status()).isEqualTo(ArticleStatus.PUBLISHED);
        assertThat(evt.version()).isEqualTo(0L);
        assertThat(evt.occurredAt()).isEqualTo(dateTimeProvider.instantOfNow);
        assertThat(evt.clientAt()).isEqualTo(clientAt);
    }
}
