package com.nm.fragmentsclean.mediaCatalogContext.read;
import java.util.UUID;
public record MediaCatalogArticleUsageView(UUID articleId,UUID revisionId,int revisionNumber,String title,String revisionStatus,String role,Integer sectionPosition,Integer imagePosition,Integer width,Integer height,boolean working,boolean published){}
