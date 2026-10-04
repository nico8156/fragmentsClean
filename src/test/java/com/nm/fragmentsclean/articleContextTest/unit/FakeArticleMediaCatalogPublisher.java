package com.nm.fragmentsclean.articleContextTest.unit;
import java.util.*;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleMediaCatalogPublisher;
public final class FakeArticleMediaCatalogPublisher implements ArticleMediaCatalogPublisher {
    public final List<UUID> published=new ArrayList<>();public void publish(UUID articleId){published.add(articleId);}
}
