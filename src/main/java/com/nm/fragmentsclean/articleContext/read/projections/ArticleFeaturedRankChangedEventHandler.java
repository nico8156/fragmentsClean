package com.nm.fragmentsclean.articleContext.read.projections;

import com.nm.fragmentsclean.articleContext.read.adapters.secondary.gateways.repositories.ArticleProjectionRepository;
import com.nm.fragmentsclean.platform.eventing.contracts.ArticleFeaturedRankChangedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public class ArticleFeaturedRankChangedEventHandler {
    private final ArticleProjectionRepository articles;
    private final ProjectionSyncPublisher sync;
    public ArticleFeaturedRankChangedEventHandler(ArticleProjectionRepository articles, ProjectionSyncPublisher sync) {
        this.articles = articles; this.sync = sync;
    }
    @Transactional public void handle(ArticleFeaturedRankChangedIntegrationEvent event) {
        articles.apply(event);
        sync.publish(ProjectionSyncEvent.projectionUpdated("articles", "collection",
                event.articleId().toString(), event.version(), event.occurredAt(), List.of("featured")));
    }
}
