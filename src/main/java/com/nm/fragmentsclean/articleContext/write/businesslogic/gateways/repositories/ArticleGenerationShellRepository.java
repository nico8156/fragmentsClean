package com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.repositories;

import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleAggregate;

public interface ArticleGenerationShellRepository {
    void save(ArticleAggregate article);
}
