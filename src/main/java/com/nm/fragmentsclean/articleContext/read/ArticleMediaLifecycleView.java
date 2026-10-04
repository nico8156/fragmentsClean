package com.nm.fragmentsclean.articleContext.read;
import java.util.UUID;
public record ArticleMediaLifecycleView(UUID mediaId,UUID articleId,String status,long usages,boolean canRetire,boolean canRestore,String retention){}
