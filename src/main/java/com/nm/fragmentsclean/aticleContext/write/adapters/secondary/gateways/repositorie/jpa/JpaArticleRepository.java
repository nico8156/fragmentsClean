package com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositorie.jpa;

import com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositorie.jpa.entities.ArticleJpaEntity;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories.ArticleRepository;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.Article;


import java.util.Optional;
import java.util.UUID;

public class JpaArticleRepository implements ArticleRepository {

    private final SpringArticleRepository springArticleRepository;

    public JpaArticleRepository(SpringArticleRepository springArticleRepository){
        this.springArticleRepository = springArticleRepository;
    }

    @Override
    public Optional<Article> byId(UUID articleId) {
        return Optional.empty();
    }

    @Override
    public void save(Article article) {

    }
//TODO finish here ...
    private Article toDomain(ArticleJpaEntity entity){
        return Article.fromSnapshot(
                new Article.ArticleSnapshot(
                        entity.getArticleId(),
                        entity.getAuthorName(),
                        entity.getLocale(),

                )
        );
    }
}
