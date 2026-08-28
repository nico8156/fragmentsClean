package com.nm.fragmentsclean.aticleContext.read.projections;

import com.nm.fragmentsclean.aticleContext.read.adapters.secondary.gateways.repositories.ArticleProjectionRepository;
import com.nm.fragmentsclean.platform.eventing.contracts.ArticleArchivedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;

public final class ArticleArchivedEventHandler {
    private final ArticleProjectionRepository repository;
    private final ProjectionSyncPublisher sync;
    public ArticleArchivedEventHandler(ArticleProjectionRepository repository, ProjectionSyncPublisher sync) {
        this.repository = repository; this.sync = sync;
    }
    public void handle(ArticleArchivedIntegrationEvent event) {
        repository.apply(event);
        sync.publish(ProjectionSyncEvent.projectionUpdated("articles", "collection",
                event.articleId().toString(), event.version(), event.occurredAt(),
                java.util.List.of("archived", "publicationStatus")));
    }
}
