package com.nm.fragmentsclean.articleContext.read.projections;

import com.nm.fragmentsclean.articleContext.read.adapters.secondary.gateways.repositories.ArticleProjectionRepository;
import com.nm.fragmentsclean.platform.eventing.contracts.ArticleRevisionPublishedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class ArticleRevisionPublishedEventHandler {

    private final ArticleProjectionRepository repository;
    private final ProjectionSyncPublisher syncPublisher;

    public ArticleRevisionPublishedEventHandler(ArticleProjectionRepository repository,
                                                ProjectionSyncPublisher syncPublisher) {
        this.repository = repository;
        this.syncPublisher = syncPublisher;
    }

    @Transactional
    public void handle(ArticleRevisionPublishedIntegrationEvent event) {
        repository.apply(event);
        syncPublisher.publish(ProjectionSyncEvent.projectionUpdated(
                "articles", "entity", event.articleId().toString(), event.version(),
                event.occurredAt(), List.of("content", "publicationStatus")));
    }
}
