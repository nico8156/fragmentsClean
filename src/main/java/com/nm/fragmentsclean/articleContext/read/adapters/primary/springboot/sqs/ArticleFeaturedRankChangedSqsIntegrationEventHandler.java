package com.nm.fragmentsclean.articleContext.read.adapters.primary.springboot.sqs;

import com.nm.fragmentsclean.articleContext.read.projections.ArticleFeaturedRankChangedEventHandler;
import com.nm.fragmentsclean.platform.eventing.contracts.ArticleFeaturedRankChangedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import org.springframework.stereotype.Component;
import static com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinations.ARTICLES_EVENTS;

@Component
public final class ArticleFeaturedRankChangedSqsIntegrationEventHandler implements SqsIntegrationEventHandler {
    private final ArticleFeaturedRankChangedEventHandler handler;
    private final SqsIntegrationEventPayloadReader reader;
    public ArticleFeaturedRankChangedSqsIntegrationEventHandler(ArticleFeaturedRankChangedEventHandler handler,
                                                                SqsIntegrationEventPayloadReader reader) {
        this.handler = handler; this.reader = reader;
    }
    @Override public SqsIntegrationEventRoute route() {
        return new SqsIntegrationEventRoute(ARTICLES_EVENTS, "article.featured_rank.changed");
    }
    @Override public void handle(IntegrationEventEnvelope envelope) {
        handler.handle(reader.read(envelope, ArticleFeaturedRankChangedIntegrationEvent.class));
    }
}
