package com.nm.fragmentsclean.aticleContext.read.adapters.secondary.gateways.repositories;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleCreatedEvent;

public interface ArticleProjectionRepository {

    void apply(ArticleCreatedEvent event);
}
