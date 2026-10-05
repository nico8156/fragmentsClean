package com.nm.fragmentsclean.articleContext.read;
import java.util.UUID;
/** A declared editorial usage; dimensions are not measured storage metadata. */
public record ArticleMediaUsageView(UUID mediaId,UUID articleId,UUID revisionId,int revisionNumber,String title,String revisionStatus,String role,Integer sectionPosition,int imagePosition,String alt,int width,int height,boolean working,boolean published,String previewUrl) {}
