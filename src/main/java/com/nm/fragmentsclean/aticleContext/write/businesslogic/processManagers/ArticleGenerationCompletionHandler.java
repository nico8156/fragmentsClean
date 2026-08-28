package com.nm.fragmentsclean.aticleContext.write.businesslogic.processManagers;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories.*;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.ArticleAuthoringObservability;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleGenerationCompletedEvent;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.generation.GeneratedArticleDraft;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;
import java.time.Instant;

@Component
public class ArticleGenerationCompletionHandler {
    private final ArticleAuthoringSagaRepository sagas; private final ArticleGenerationRunRepository runs; private final ArticleGenerationArtifactRepository artifacts; private final ArticleRevisionMaterializer materializer; private final DomainEventPublisher events; private final ArticleAuthoringObservability observability;
    public ArticleGenerationCompletionHandler(ArticleAuthoringSagaRepository sagas, ArticleGenerationRunRepository runs, ArticleGenerationArtifactRepository artifacts, ArticleRevisionMaterializer materializer, DomainEventPublisher events, ArticleAuthoringObservability observability) { this.sagas=sagas; this.runs=runs; this.artifacts=artifacts; this.materializer=materializer; this.events=events; this.observability=observability; }
    @Transactional
    public void complete(ArticleGenerationLeaseClaimer.Work work, String provider, String responseId, String model, String schemaVersion, GeneratedArticleDraft draft, Instant now) {
        if (draft == null) throw new IllegalArgumentException("Validated generation draft is required");
        if (draft.coverImage()==null || draft.sections().stream().anyMatch(section->section.content().images().size()!=1)) throw new IllegalArgumentException("Generated article media is incomplete");
        var saga=sagas.byId(work.saga().sagaId()).orElseThrow();
        var current=saga.snapshot();
        if (current.state()!=ArticleAuthoringSagaState.GENERATING || !work.run().workerId().equals(current.leaseOwner())) return;
        var run=runs.byId(work.run().runId()).orElseThrow(); run.succeed(provider,responseId,model,schemaVersion,now);
        artifacts.save(run.snapshot().runId(), current.sagaId(), current.articleId(), current.revisionId(), schemaVersion, draft);
        materializer.materialize(current.articleId(), current.revisionId(), draft, now);
        saga.startValidation(now);
        sagas.save(saga);
        saga.markReadyForReview(now);
        sagas.save(saga);
        runs.save(run);
        var s=saga.snapshot(); events.publish(new ArticleGenerationCompletedEvent(java.util.UUID.randomUUID(), s.sagaId(), s.articleId(), s.revisionId(), work.run().runId(), provider,responseId,model,schemaVersion,s.version(),now));
        observability.generationCompleted();
    }
}
