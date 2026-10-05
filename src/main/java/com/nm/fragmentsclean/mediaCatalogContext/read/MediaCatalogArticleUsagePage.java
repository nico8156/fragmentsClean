package com.nm.fragmentsclean.mediaCatalogContext.read;
import java.util.List;
public record MediaCatalogArticleUsagePage(List<MediaCatalogArticleUsageView> items,String nextCursor){public MediaCatalogArticleUsagePage{items=List.copyOf(items);}}
