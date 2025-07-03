package com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositorie.fake;

import com.nm.fragmentsclean.aticleContext.Article;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories.ArticleRepository;

import java.util.ArrayList;
import java.util.List;

public class FakeArticleRepository implements ArticleRepository {

    public List<Article> articles = new ArrayList<>();

    @Override
    public void save(Article article) {articles.add(article);}
}
