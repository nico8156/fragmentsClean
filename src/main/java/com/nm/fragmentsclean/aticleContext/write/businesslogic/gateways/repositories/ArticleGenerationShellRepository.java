package com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleAggregate;

public interface ArticleGenerationShellRepository {
    void save(ArticleAggregate article);
}
