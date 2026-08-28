package com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleAggregate;

import java.util.Optional;
import java.util.UUID;

public interface ArticleAggregateRepository {

    Optional<ArticleAggregate> byId(UUID articleId);

    void save(ArticleAggregate article);
}
