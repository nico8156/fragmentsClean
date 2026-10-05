package com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class ReplayArticleMediaCatalog {
    private final ArticleMediaCatalogScan scan;private final ArticleMediaCatalogPublisher publisher;
    public ReplayArticleMediaCatalog(ArticleMediaCatalogScan scan,ArticleMediaCatalogPublisher publisher){this.scan=scan;this.publisher=publisher;}
    @Transactional public int nextBatch(){
        var batch=scan.lockNext(100);if(!batch.due())return 0;
        batch.articleIds().forEach(publisher::publish);
        scan.advance(batch.articleIds().isEmpty()?null:batch.articleIds().getLast(),batch.articleIds().size()<100);return batch.articleIds().size();
    }
}
