package com.nm.fragmentsclean.aticleContext.write.businesslogic.processManagers;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.ArticleAuthoringObservability;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories.ArticleAuthoringSagaRepository;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories.ArticleGenerationRunRepository;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleImageRef;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.generation.ArticleEditorialTag;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.generation.GeneratedArticleDraft;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.generation.GeneratedArticleSection;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleGenerationCompletionHandlerTest {

    @Test
    void persists_each_saga_transition_with_its_expected_optimistic_version() {
        var now = Instant.parse("2026-08-28T14:00:00Z");
        var saga = requestedSaga(now);
        var run = ArticleGenerationRun.start(UUID.randomUUID(), saga.snapshot().sagaId(), 1,
                saga.snapshot().leaseOwner(), now);
        var sagas = new RecordingSagaRepository(saga);
        var runs = new InMemoryRunRepository(run);
        var publishedEvents = new ArrayList<Object>();
        var handler = new ArticleGenerationCompletionHandler(
                sagas,
                runs,
                (runId, sagaId, articleId, revisionId, schemaVersion, draft) -> { },
                (articleId, revisionId, draft, completedAt) -> { },
                publishedEvents::add,
                ArticleAuthoringObservability.noop());

        handler.complete(
                new ArticleGenerationLeaseClaimer.Work(saga.snapshot(), run.snapshot()),
                "openai", "response-1", "gpt-4o-mini", "article-generation.v1", enrichedDraft(), now.plusSeconds(30));

        assertThat(sagas.savedVersions).containsExactly(3L, 4L);
        assertThat(saga.snapshot().state()).isEqualTo(ArticleAuthoringSagaState.READY_FOR_REVIEW);
        assertThat(runs.run.snapshot().status()).isEqualTo(ArticleGenerationRun.Status.SUCCEEDED);
        assertThat(publishedEvents).hasSize(1);
    }

    private static ArticleAuthoringSaga requestedSaga(Instant now) {
        var saga = ArticleAuthoringSaga.request(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Culture café", ArticleAuthoringTrigger.MANUAL, now);
        saga.enqueueGeneration(now.plusSeconds(1));
        saga.claimGeneration("worker-1", now.plusSeconds(2), now.plus(Duration.ofMinutes(5)));
        return saga;
    }

    private static GeneratedArticleDraft enrichedDraft() {
        return GeneratedArticleDraft.from("Titre", "Introduction", "Conclusion", "Couverture",
                        List.of(section("A"), section("B"), section("C")),
                        List.of(ArticleEditorialTag.DECOUVERTE))
                .withGeneratedImages(
                        image("cover", 1024, 1536, "Cover"),
                        List.of(image("a", 1536, 1024, "A"), image("b", 1536, 1024, "B"),
                                image("c", 1536, 1024, "C")));
    }

    private static GeneratedArticleSection section(String heading) {
        return GeneratedArticleSection.from(heading, "Paragraphe " + heading, "Illustrer " + heading);
    }

    private static ArticleImageRef image(String ref, int width, int height, String alt) {
        return ArticleImageRef.from(ref, width, height, alt);
    }

    private static final class RecordingSagaRepository implements ArticleAuthoringSagaRepository {
        private final ArticleAuthoringSaga saga;
        private final List<Long> savedVersions = new ArrayList<>();

        private RecordingSagaRepository(ArticleAuthoringSaga saga) { this.saga = saga; }
        @Override public Optional<ArticleAuthoringSaga> byId(UUID sagaId) { return Optional.of(saga); }
        @Override public void save(ArticleAuthoringSaga saga) { savedVersions.add(saga.snapshot().version()); }
    }

    private static final class InMemoryRunRepository implements ArticleGenerationRunRepository {
        private final ArticleGenerationRun run;
        private InMemoryRunRepository(ArticleGenerationRun run) { this.run = run; }
        @Override public Optional<ArticleGenerationRun> byId(UUID runId) { return Optional.of(run); }
        @Override public void save(ArticleGenerationRun run) { }
    }
}
