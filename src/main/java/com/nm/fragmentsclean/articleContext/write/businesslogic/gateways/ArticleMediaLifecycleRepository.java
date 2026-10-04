package com.nm.fragmentsclean.articleContext.write.businesslogic.gateways;
import java.util.*;import java.time.Instant;import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleMediaLifecycle;
public interface ArticleMediaLifecycleRepository {
 Optional<ArticleMediaLifecycle> lock(UUID mediaId);
 void change(UUID mediaId,String status,Instant at);
}
