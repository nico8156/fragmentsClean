package com.nm.fragmentsclean.articleContext.read.adapters.primary.springboot.sqs;

import com.nm.fragmentsclean.articleContext.read.projections.ArticleArchivedEventHandler;
import com.nm.fragmentsclean.platform.eventing.contracts.ArticleArchivedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import org.springframework.stereotype.Component;

import static com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinations.ARTICLES_EVENTS;

@Component
public final class ArticleArchivedSqsIntegrationEventHandler implements SqsIntegrationEventHandler {
    private final ArticleArchivedEventHandler handler;
    private final SqsIntegrationEventPayloadReader reader;
    public ArticleArchivedSqsIntegrationEventHandler(ArticleArchivedEventHandler handler,
                                                     SqsIntegrationEventPayloadReader reader) {
        this.handler = handler; this.reader = reader;
    }
    @Override public SqsIntegrationEventRoute route() {
        return new SqsIntegrationEventRoute(ARTICLES_EVENTS, "article.archived");
    }
    @Override public void handle(IntegrationEventEnvelope envelope) {
        handler.handle(reader.read(envelope, ArticleArchivedIntegrationEvent.class));
    }
}
