package com.nm.fragmentsclean.articleContextTest.unit;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.processManagers.ArticleAuthoringSaga;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.processManagers.ArticleAuthoringTrigger;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleAuthoringRecoveryTest {
    @Test
    void an_expired_worker_lease_can_be_recovered_and_claimed_again() {
        var start = Instant.parse("2026-08-27T10:00:00Z");
        var saga = ArticleAuthoringSaga.request(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Le café filtre", ArticleAuthoringTrigger.SCHEDULED, start);
        saga.enqueueGeneration(start);
        saga.claimGeneration("worker-1", start, start.plusSeconds(60));

        saga.recoverExpiredGeneration(start.plusSeconds(61));
        saga.claimGeneration("worker-2", start.plusSeconds(61), start.plusSeconds(121));

        assertThat(saga.snapshot().leaseOwner()).isEqualTo("worker-2");
        assertThat(saga.snapshot().generationAttempts()).isEqualTo(2);
    }
}
