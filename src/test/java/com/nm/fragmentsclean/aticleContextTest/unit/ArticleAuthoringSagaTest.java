package com.nm.fragmentsclean.aticleContextTest.unit;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleDomainException;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.processManagers.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ArticleAuthoringSagaTest {
    private static final Instant T0 = Instant.parse("2026-08-27T10:00:00Z");
    private final UUID article = UUID.randomUUID();
    private final UUID revision = UUID.randomUUID();

    @Test
    void modelsTheHappyPathAndKeepsEachTransitionExplicit() {
        var saga = ArticleAuthoringSaga.request(UUID.randomUUID(), article, revision, "origines", ArticleAuthoringTrigger.MANUAL, T0);
        saga.enqueueGeneration(T0.plusSeconds(1));
        saga.claimGeneration("worker-1", T0.plusSeconds(2), T0.plusSeconds(60));
        saga.startValidation(T0.plusSeconds(3));
        saga.markReadyForReview(T0.plusSeconds(4));
        saga.requestNotification(T0.plusSeconds(5));
        saga.markNotified(T0.plusSeconds(6));
        saga.requestPublication(T0.plusSeconds(7));
        saga.markPublished(T0.plusSeconds(8));

        assertEquals(ArticleAuthoringSagaState.PUBLISHED, saga.snapshot().state());
        assertEquals(8, saga.snapshot().version());
    }

    @Test
    void rejectsIllegalTransitionsAndProtectsAnActiveLease() {
        var saga = ArticleAuthoringSaga.request(UUID.randomUUID(), article, revision, "origines", ArticleAuthoringTrigger.SCHEDULED, T0);
        assertThrows(ArticleDomainException.class, () -> saga.markReadyForReview(T0));
        saga.enqueueGeneration(T0.plusSeconds(1));
        saga.claimGeneration("worker-1", T0.plusSeconds(2), T0.plusSeconds(10));
        assertThrows(ArticleDomainException.class, () -> saga.claimGeneration("worker-2", T0.plusSeconds(3), T0.plusSeconds(20)));
        assertEquals(ArticleAuthoringSagaState.GENERATING, saga.snapshot().state());
    }

    @Test
    void canBeReclaimedAfterWorkerLossAndReconstitutedFromPersistence() {
        var saga = ArticleAuthoringSaga.request(UUID.randomUUID(), article, revision, "origines", ArticleAuthoringTrigger.MANUAL, T0);
        saga.enqueueGeneration(T0.plusSeconds(1));
        saga.claimGeneration("worker-1", T0.plusSeconds(2), T0.plusSeconds(10));
        saga.recoverExpiredGeneration(T0.plusSeconds(10));
        assertEquals(ArticleAuthoringSagaState.GENERATION_PENDING, saga.snapshot().state());
        saga.claimGeneration("worker-2", T0.plusSeconds(11), T0.plusSeconds(30));

        var restored = ArticleAuthoringSaga.reconstitute(saga.snapshot());
        assertEquals(saga.snapshot(), restored.snapshot());
        assertEquals(2, restored.snapshot().generationAttempts());
    }
}
