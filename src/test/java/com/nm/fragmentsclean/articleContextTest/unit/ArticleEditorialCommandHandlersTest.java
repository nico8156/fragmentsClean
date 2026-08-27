package com.nm.fragmentsclean.articleContextTest.unit;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories.ArticleAggregateRepository;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleAggregate;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleContent;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleIntroduction;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleParagraph;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleRevision;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleSection;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleTitle;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article.CreateArticleDraftCommand;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article.CreateArticleDraftCommandHandler;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article.PublishArticleRevisionCommand;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article.PublishArticleRevisionCommandHandler;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article.SubmitArticleRevisionForReviewCommand;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article.SubmitArticleRevisionForReviewCommandHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.CommandStatusRecorder;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleEditorialCommandHandlersTest {

    private static final Instant NOW = Instant.parse("2026-08-27T10:00:00Z");
    private static final UUID ARTICLE_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID REVISION_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID AUTHOR_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID CREATE_COMMAND_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID REVIEW_COMMAND_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final UUID PUBLISH_COMMAND_ID = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

    @Test
    void handlers_persist_transition_publish_event_and_record_command_status() {
        var repository = new FakeArticleAggregateRepository();
        var publisher = new RecordingPublisher();
        var status = new RecordingCommandStatus();
        var content = content();

        new CreateArticleDraftCommandHandler(repository, publisher, () -> NOW, status).execute(
                new CreateArticleDraftCommand(CREATE_COMMAND_ID, NOW, ARTICLE_ID, REVISION_ID,
                        "guide-cafe", "fr-FR", AUTHOR_ID, "Studio", content));
        new SubmitArticleRevisionForReviewCommandHandler(repository, publisher, () -> NOW.plusSeconds(60), status)
                .execute(new SubmitArticleRevisionForReviewCommand(REVIEW_COMMAND_ID, NOW, ARTICLE_ID));
        new PublishArticleRevisionCommandHandler(repository, publisher, () -> NOW.plusSeconds(120), status)
                .execute(new PublishArticleRevisionCommand(PUBLISH_COMMAND_ID, NOW, ARTICLE_ID, REVISION_ID));

        var article = repository.byId(ARTICLE_ID).orElseThrow();
        assertThat(article.lifecycle().name()).isEqualTo("PUBLISHED");
        assertThat(article.publishedRevisionId()).isEqualTo(REVISION_ID);
        assertThat(publisher.events).hasSize(3);
        assertThat(status.applied).containsExactly(CREATE_COMMAND_ID, REVIEW_COMMAND_ID, PUBLISH_COMMAND_ID);
    }

    private ArticleContent content() {
        return ArticleContent.draft(
                ArticleTitle.from("Guide café"),
                ArticleIntroduction.from("Une introduction."),
                List.of(ArticleSection.draft("Comprendre")
                        .withParagraph(ArticleParagraph.from("Un paragraphe."))),
                ArticleParagraph.from("Une conclusion."));
    }

    private static final class FakeArticleAggregateRepository implements ArticleAggregateRepository {
        private ArticleAggregate article;

        @Override
        public Optional<ArticleAggregate> byId(UUID articleId) {
            return article == null || !article.id().equals(articleId) ? Optional.empty() : Optional.of(article);
        }

        @Override
        public void save(ArticleAggregate article) {
            this.article = article;
        }
    }

    private static final class RecordingPublisher implements DomainEventPublisher {
        private final List<DomainEvent> events = new ArrayList<>();

        @Override
        public void publish(DomainEvent event) {
            events.add(event);
        }
    }

    private static final class RecordingCommandStatus implements CommandStatusRecorder {
        private final List<UUID> applied = new ArrayList<>();

        @Override
        public void markApplied(UUID commandId, String aggregateType, String aggregateId,
                                String eventType, Instant appliedAt) {
            applied.add(commandId);
        }

        @Override
        public boolean isApplied(UUID commandId) {
            return applied.contains(commandId);
        }
    }
}
