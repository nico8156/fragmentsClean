package com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositorie.fake;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories.ArticleRepository;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.Article;

import java.util.*;

public class FakeArticleRepository implements ArticleRepository {

    private final Map<UUID, Article.ArticleSnapshot> storage = new LinkedHashMap<>();

    @Override
    public Optional<Article> byId(UUID articleId) {
        var snap = storage.get(articleId);
        if (snap == null) {
            return Optional.empty();
        }
        return Optional.of(Article.fromSnapshot(snap));
    }

    @Override
    public void save(Article article) {
        storage.put(article.id(), article.toSnapshot());
    }

    // Helpers pour les tests
    public List<Article.ArticleSnapshot> allSnapshots() {
        return new ArrayList<>(storage.values());
    }

    public void clear() {
        storage.clear();
    }
}
