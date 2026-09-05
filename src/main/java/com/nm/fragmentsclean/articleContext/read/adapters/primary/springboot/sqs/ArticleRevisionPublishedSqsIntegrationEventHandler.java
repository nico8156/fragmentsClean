package com.nm.fragmentsclean.articleContext.read.adapters.primary.springboot.sqs;

import static com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinations.ARTICLES_EVENTS;

import com.nm.fragmentsclean.articleContext.read.projections.ArticleRevisionPublishedEventHandler;
import com.nm.fragmentsclean.platform.eventing.contracts.ArticleRevisionPublishedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventHandler;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventPayloadReader;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventRoute;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import org.springframework.stereotype.Component;

@Component
public final class ArticleRevisionPublishedSqsIntegrationEventHandler implements SqsIntegrationEventHandler {

    private final ArticleRevisionPublishedEventHandler handler;
    private final SqsIntegrationEventPayloadReader payloadReader;

    public ArticleRevisionPublishedSqsIntegrationEventHandler(ArticleRevisionPublishedEventHandler handler,
                                                              SqsIntegrationEventPayloadReader payloadReader) {
        this.handler = handler;
        this.payloadReader = payloadReader;
    }

    @Override
    public SqsIntegrationEventRoute route() {
        return new SqsIntegrationEventRoute(ARTICLES_EVENTS, "article.revision.published");
    }

    @Override
    public void handle(IntegrationEventEnvelope envelope) {
        handler.handle(payloadReader.read(envelope, ArticleRevisionPublishedIntegrationEvent.class));
    }
}
