package com.nm.fragmentsclean.articleContext.read.adapters.primary.springboot.sqs;

import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleCreatedEvent;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleStatus;
import com.nm.fragmentsclean.platform.eventing.contracts.ArticleCreatedIntegrationEvent;

final class ArticleIntegrationEventAcl {
    private ArticleIntegrationEventAcl() { }

    static ArticleCreatedEvent toLocalEvent(ArticleCreatedIntegrationEvent event) {
        return new ArticleCreatedEvent(event.eventId(), event.commandId(), event.articleId(), event.slug(), event.locale(),
                event.authorId(), event.authorName(), event.title(), event.intro(), event.blocksJson(), event.conclusion(),
                event.coverUrl(), event.coverWidth(), event.coverHeight(), event.coverAlt(), event.tags(), event.readingTimeMin(),
                event.coffeeIds(), ArticleStatus.valueOf(event.status()), event.version(), event.occurredAt(), event.clientAt());
    }
}
