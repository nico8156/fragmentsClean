package com.nm.fragmentsclean.articleContext.read.adapters.primary.springboot.sqs;

import static com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinations.ARTICLES_EVENTS;

import com.nm.fragmentsclean.articleContext.read.projections.ArticleCreatedEventHandler;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleCreatedEvent;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventHandler;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventPayloadReader;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventRoute;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import org.springframework.stereotype.Component;

@Component
public class ArticleCreatedSqsIntegrationEventHandler implements SqsIntegrationEventHandler {

    private final ArticleCreatedEventHandler handler;
    private final SqsIntegrationEventPayloadReader payloadReader;

    public ArticleCreatedSqsIntegrationEventHandler(
            ArticleCreatedEventHandler handler,
            SqsIntegrationEventPayloadReader payloadReader) {
        this.handler = handler;
        this.payloadReader = payloadReader;
    }

    @Override
    public SqsIntegrationEventRoute route() {
        return new SqsIntegrationEventRoute(ARTICLES_EVENTS, "article.created");
    }

    @Override
    public void handle(IntegrationEventEnvelope envelope) {
        handler.handle(payloadReader.read(envelope, ArticleCreatedEvent.class));
    }
}
