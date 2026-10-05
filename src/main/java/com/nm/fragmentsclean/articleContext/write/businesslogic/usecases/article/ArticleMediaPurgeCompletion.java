package com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.*;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleMediaLifecycle;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
@Component public class ArticleMediaPurgeCompletion {
 private final ArticleMediaLifecycleRepository repository;private final ArticleMediaCatalogPublisher catalogue;private final DateTimeProvider clock;
 public ArticleMediaPurgeCompletion(ArticleMediaLifecycleRepository repository,ArticleMediaCatalogPublisher catalogue,DateTimeProvider clock){this.repository=repository;this.catalogue=catalogue;this.clock=clock;}
 @Transactional public Optional<ArticleMediaLifecycle> pending(UUID id){return repository.lock(id).filter(m->"DELETION_PENDING".equals(m.status())&&m.usages()==0);}
 @Transactional public void complete(UUID id){var item=pending(id);if(item.isEmpty())return;var media=item.get();repository.change(id,"DELETED",clock.now());catalogue.publish(media.articleId());}
}
