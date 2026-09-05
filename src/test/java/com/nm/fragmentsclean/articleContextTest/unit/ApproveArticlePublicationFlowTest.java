package com.nm.fragmentsclean.articleContextTest.unit;

import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleReviewApproval;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleReviewApprovalValidator;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.repositories.ArticleAggregateRepository;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.repositories.ArticleAuthoringSagaRepository;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.*;
import com.nm.fragmentsclean.articleContext.write.businesslogic.processManagers.ArticleAuthoringSaga;
import com.nm.fragmentsclean.articleContext.write.businesslogic.processManagers.ArticleAuthoringTrigger;
import com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article.*;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.CommandStatusRecorder;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ApproveArticlePublicationFlowTest {
    @Test
    void consumes_approval_submits_and_publishes_the_exact_revision_and_completes_the_saga() {
        var now = Instant.parse("2026-08-28T08:00:00Z");
        var articleId = UUID.randomUUID();
        var revisionId = UUID.randomUUID();
        var sagaId = UUID.randomUUID();
        var article = ArticleAggregate.draft(articleId, "cafe-filtre", "fr-FR", UUID.randomUUID(), "Studio",
                ArticleRevision.draft(revisionId, draft(), now.minusSeconds(60)), now.minusSeconds(60));
        var saga = readySaga(sagaId, articleId, revisionId, now.minusSeconds(30));
        var articles = new FakeArticles(article);
        var sagas = new FakeSagas(saga);
        var statuses = new FakeStatuses();
        DomainEventPublisher events = event -> { };
        var submit = new SubmitArticleRevisionForReviewCommandHandler(articles, events, () -> now, statuses);
        var publish = new PublishArticleRevisionCommandHandler(articles, events, () -> now, statuses,
                ignored -> 29, new ArticlePublicationPolicy());
        var bus = new CommandBus();
        bus.registerCommandHandlers(java.util.List.of(submit, publish));
        var approval = new ArticleReviewApproval(UUID.randomUUID(), sagaId, articleId, revisionId,
                "hash", now.minusSeconds(10), now.plusSeconds(600), null);
        var approvals = new FakeApprovals(approval);

        UUID publicationCommandId = new ApproveArticlePublication(approvals, bus, () -> now, sagas).execute("token");

        assertThat(publicationCommandId).isNotNull();
        assertThat(approvals.consumed).isTrue();
        assertThat(articles.article.lifecycle()).isEqualTo(ArticleLifecycle.PUBLISHED);
        assertThat(articles.article.publishedRevisionId()).isEqualTo(revisionId);
        assertThat(sagas.saga.snapshot().state().name()).isEqualTo("PUBLISHED");
    }

    private static ArticleContent content() {
        return ArticleContent.draft(ArticleTitle.from("Le café filtre"), ArticleIntroduction.from("Introduction"),
                java.util.List.of(section("Méthode"), section("Origines"), section("Dégustation")),
                ArticleParagraph.from("Conclusion"));
    }

    private static ArticleSection section(String heading) {
        return ArticleSection.draft(heading)
                .withParagraph(ArticleParagraph.from("Paragraphe"))
                .withImage(ArticleImageRef.from("s3://articles/" + heading.toLowerCase() + ".jpg", 1200, 800, heading));
    }

    private static ArticleRevisionDraft draft() {
        return ArticleRevisionDraft.editable(content(),
                ArticleImageRef.from("s3://articles/cover.jpg", 1200, 800, "Couverture"),
                java.util.List.of(com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation.ArticleEditorialTag.DECOUVERTE));
    }

    private static ArticleAuthoringSaga readySaga(UUID sagaId, UUID articleId, UUID revisionId, Instant now) {
        var saga = ArticleAuthoringSaga.request(sagaId, articleId, revisionId, "Le café filtre",
                ArticleAuthoringTrigger.MANUAL, now);
        saga.enqueueGeneration(now);
        saga.claimGeneration("worker", now, now.plusSeconds(60));
        saga.startValidation(now);
        saga.markReadyForReview(now);
        return saga;
    }

    private static final class FakeArticles implements ArticleAggregateRepository {
        private ArticleAggregate article;
        private FakeArticles(ArticleAggregate article) { this.article = article; }
        public Optional<ArticleAggregate> byId(UUID id) { return article.id().equals(id) ? Optional.of(article) : Optional.empty(); }
        public void save(ArticleAggregate article) { this.article = article; }
    }

    private static final class FakeSagas implements ArticleAuthoringSagaRepository {
        private ArticleAuthoringSaga saga;
        private FakeSagas(ArticleAuthoringSaga saga) { this.saga = saga; }
        public Optional<ArticleAuthoringSaga> byId(UUID id) { return saga.snapshot().sagaId().equals(id) ? Optional.of(saga) : Optional.empty(); }
        public void save(ArticleAuthoringSaga saga) { this.saga = saga; }
    }

    private static final class FakeApprovals implements ArticleReviewApprovalValidator {
        private final ArticleReviewApproval approval;
        private boolean consumed;
        private FakeApprovals(ArticleReviewApproval approval) { this.approval = approval; }
        public ArticleReviewApproval validate(String token, Instant now) { return approval; }
        public boolean consume(UUID approvalId, Instant consumedAt) { consumed = approval.approvalId().equals(approvalId); return consumed; }
    }

    private static final class FakeStatuses implements CommandStatusRecorder {
        private final java.util.Set<UUID> applied = new java.util.HashSet<>();
        public void markApplied(UUID commandId, String aggregateType, String aggregateId, String eventType, Instant appliedAt) { applied.add(commandId); }
        public boolean isApplied(UUID commandId) { return applied.contains(commandId); }
    }
}
