package com.nm.fragmentsclean.aticleContext.read.projections;


import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleCreatedEvent;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;
import org.springframework.stereotype.Component;

@Component
public class ArticleCreatedEventHandler implements EventHandler<ArticleCreatedEvent> {

    private final ArticleProjectionHandler projectionHandler;

    public ArticleCreatedEventHandler(ArticleProjectionHandler projectionHandler) {
        this.projectionHandler = projectionHandler;
    }

    @Override
    public void handle(ArticleCreatedEvent event) {
        projectionHandler.on(event);
    }
}
