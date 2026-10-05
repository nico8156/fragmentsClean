package com.nm.fragmentsclean.articleContext.read;
import java.util.Optional;import java.util.UUID;
public interface ArticleMediaLifecycleReadRepository {Optional<ArticleMediaLifecycleView> byId(UUID mediaId);}
