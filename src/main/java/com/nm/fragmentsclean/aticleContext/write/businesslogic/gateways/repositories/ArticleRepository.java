package com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.Article;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArticleRepository {

    Optional<Article> byId(UUID articleId);

    void save(Article article);

    /**
     * Tous les articles publiés (status = PUBLISHED)
     * → utile pour un read-side simple ou du batch.
     */
    List<Article> findAllPublished();
}
