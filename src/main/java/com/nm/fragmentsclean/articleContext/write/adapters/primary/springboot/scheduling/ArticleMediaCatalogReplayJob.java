package com.nm.fragmentsclean.articleContext.write.adapters.primary.springboot.scheduling;
import com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article.ReplayArticleMediaCatalog;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component
@ConditionalOnProperty(name="fragments.media-catalog.replay.enabled",havingValue="true")
public final class ArticleMediaCatalogReplayJob {
    private final ReplayArticleMediaCatalog replay;
    public ArticleMediaCatalogReplayJob(ReplayArticleMediaCatalog replay){this.replay=replay;}
    @Scheduled(fixedDelayString="${fragments.media-catalog.replay.delay-ms:15000}")public void tick(){replay.nextBatch();}
}
