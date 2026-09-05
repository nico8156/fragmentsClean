package com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.repositories;

import com.nm.fragmentsclean.articleContext.write.businesslogic.processManagers.ArticleAuthoringSaga;

import java.util.Optional;
import java.util.UUID;

public interface ArticleAuthoringSagaRepository {
    Optional<ArticleAuthoringSaga> byId(UUID sagaId);
    void save(ArticleAuthoringSaga saga);
}
