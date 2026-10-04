package com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article;
import org.springframework.stereotype.Component;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.*;
@Component public final class CleanArticleMediaObjects {
 private final ArticleMediaLifecycleRepository repository;private final ArticleMediaObjectDeletion objects;private final ArticleMediaPurgeCompletion completion;
 public CleanArticleMediaObjects(ArticleMediaLifecycleRepository repository,ArticleMediaObjectDeletion objects,ArticleMediaPurgeCompletion completion){this.repository=repository;this.objects=objects;this.completion=completion;}
 public void run(int limit){for(var id:repository.pendingPurgeIds(limit)){var item=completion.pending(id);if(item.isEmpty())continue;var media=item.get();objects.delete(media.articleId(),media.reference());completion.complete(id);}}
}
