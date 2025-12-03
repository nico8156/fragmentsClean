package com.nm.fragmentsclean.aticleContext.read.projections;

import com.nm.fragmentsclean.aticleContext.read.adapters.secondary.gateways.repositories.ArticleProjectionRepository;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleCreatedEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;
import org.springframework.stereotype.Component;

@Component
public class ArticleCreatedEventHandler implements EventHandler<ArticleCreatedEvent> {

    private final ArticleProjectionRepository projectionRepository;

    public ArticleCreatedEventHandler(ArticleProjectionRepository projectionRepository) {
        this.projectionRepository = projectionRepository;
    }

    public void handle(ArticleCreatedEvent event) {
        projectionRepository.apply(event);
    }
}
