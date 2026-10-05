package com.nm.fragmentsclean.articleContext.write.adapters.primary.springboot.scheduling;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article.CleanArticleMediaObjects;
@Component @ConditionalOnProperty(name="fragments.article.media-purge.enabled",havingValue="true")
public final class ArticleMediaCleanupJob {
 private final CleanArticleMediaObjects cleanup;
 public ArticleMediaCleanupJob(CleanArticleMediaObjects cleanup){this.cleanup=cleanup;}
 @Scheduled(fixedDelayString="${fragments.article.media-purge.delay-ms:600000}")public void clean(){cleanup.run(100);}
}
