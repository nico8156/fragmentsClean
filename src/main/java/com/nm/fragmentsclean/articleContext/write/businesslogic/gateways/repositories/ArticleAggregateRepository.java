package com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.repositories;

import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleAggregate;

import java.util.Optional;
import java.util.UUID;

public interface ArticleAggregateRepository {

    Optional<ArticleAggregate> byId(UUID articleId);

    /** Production adapters lock the aggregate until the command transaction completes. */
    default Optional<ArticleAggregate> byIdForUpdate(UUID articleId) { return byId(articleId); }

    void save(ArticleAggregate article);
}
