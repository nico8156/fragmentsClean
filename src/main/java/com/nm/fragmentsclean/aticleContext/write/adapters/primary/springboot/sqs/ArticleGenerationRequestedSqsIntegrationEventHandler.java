package com.nm.fragmentsclean.aticleContext.write.adapters.primary.springboot.sqs;

import static com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinations.ARTICLES_EVENTS;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.ArticleGenerationProvider;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.ArticleAuthoringObservability;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.generation.ArticleSubject;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.processManagers.ArticleGenerationCompletionHandler;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.processManagers.ArticleGenerationLeaseClaimer;
import com.nm.fragmentsclean.platform.eventing.contracts.ArticleGenerationRequestedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventHandler;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventPayloadReader;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventRoute;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Keeps the provider call outside the transaction that claims the saga lease. */
@Component
@ConditionalOnProperty(name = "fragments.article.generation.openai.enabled", havingValue = "true")
public final class ArticleGenerationRequestedSqsIntegrationEventHandler implements SqsIntegrationEventHandler {
    private final SqsIntegrationEventPayloadReader payloadReader;
    private final ArticleGenerationLeaseClaimer claimer;
    private final ArticleGenerationProvider provider;
    private final com.nm.fragmentsclean.aticleContext.write.businesslogic.processManagers.ArticleGeneratedMediaService media;
    private final ArticleGenerationCompletionHandler completer;
    private final ArticleAuthoringObservability observability;

    public ArticleGenerationRequestedSqsIntegrationEventHandler(SqsIntegrationEventPayloadReader payloadReader,
            ArticleGenerationLeaseClaimer claimer, ArticleGenerationProvider provider, com.nm.fragmentsclean.aticleContext.write.businesslogic.processManagers.ArticleGeneratedMediaService media, ArticleGenerationCompletionHandler completer, ArticleAuthoringObservability observability) {
        this.payloadReader=payloadReader; this.claimer=claimer; this.provider=provider; this.media=media; this.completer=completer; this.observability=observability;
    }
    @Override public SqsIntegrationEventRoute route() { return new SqsIntegrationEventRoute(ARTICLES_EVENTS, "article.generation.requested"); }
    @Override public void handle(IntegrationEventEnvelope envelope) {
        var request=payloadReader.read(envelope, ArticleGenerationRequestedIntegrationEvent.class);
        var now=Instant.now(); var work=claimer.claim(request.sagaId(), "article-generation-"+UUID.randomUUID(), now, Duration.ofMinutes(5));
        try {
            var result=provider.generate(new ArticleGenerationProvider.Request(request.sagaId(), ArticleSubject.from(request.theme()), request.locale()));
            var enriched=media.generate(request.sagaId(),request.articleId(),result.draft());
            completer.complete(work, "openai", result.providerResponseId(), result.model(), result.schemaVersion(), enriched, Instant.now());
        } catch (RuntimeException failure) {
            observability.generationFailed(failure.getClass().getSimpleName());
            throw failure;
        }
    }
}
