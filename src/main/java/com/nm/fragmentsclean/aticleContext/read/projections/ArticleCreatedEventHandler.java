package com.nm.fragmentsclean.aticleContext.read.projections;

import com.nm.fragmentsclean.aticleContext.read.adapters.secondary.gateways.repositories.ArticleProjectionRepository;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleCreatedEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ArticleCreatedEventHandler implements EventHandler<ArticleCreatedEvent> {

    private final ArticleProjectionRepository projectionRepository;
    private static final Logger log = LoggerFactory.getLogger(ArticleCreatedEventHandler.class);

    public ArticleCreatedEventHandler(ArticleProjectionRepository projectionRepository) {
        this.projectionRepository = projectionRepository;
    }

    public void handle(ArticleCreatedEvent event) {
        log.info("Applying ArticleCreatedEvent to projection for articleId={}", event.articleId().toString());
        projectionRepository.apply(event);
    }
}
