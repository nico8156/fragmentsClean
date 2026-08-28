package com.nm.fragmentsclean.aticleContext.write.businesslogic.processManagers;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories.*;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.ArticleAuthoringObservability;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;
import java.time.*;
import java.util.UUID;

@Component
public class ArticleGenerationLeaseClaimer {
    private final ArticleAuthoringSagaRepository sagas; private final ArticleGenerationRunRepository runs;
    private final ArticleAuthoringObservability observability;
    public ArticleGenerationLeaseClaimer(ArticleAuthoringSagaRepository sagas, ArticleGenerationRunRepository runs,
                                         ArticleAuthoringObservability observability) {
        this.sagas=sagas; this.runs=runs; this.observability=observability;
    }
    @Transactional
    public Work claim(UUID sagaId, String workerId, Instant now, Duration lease) {
        var saga=sagas.byId(sagaId).orElseThrow(() -> new IllegalStateException("Unknown article authoring saga"));
        boolean recovered = saga.snapshot().state() == ArticleAuthoringSagaState.GENERATING && saga.leaseExpired(now);
        saga.recoverExpiredGeneration(now); saga.claimGeneration(workerId, now, now.plus(lease));
        var run=ArticleGenerationRun.start(UUID.randomUUID(), sagaId, saga.snapshot().generationAttempts(), workerId, now);
        sagas.save(saga); runs.save(run);
        observability.leaseClaimed(recovered);
        return new Work(saga.snapshot(), run.snapshot());
    }
    public record Work(ArticleAuthoringSaga.Snapshot saga, ArticleGenerationRun.Snapshot run) { }
}
