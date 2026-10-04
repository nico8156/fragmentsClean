package com.nm.fragmentsclean.mediaCatalogContext.read;
import java.util.Optional;
public interface MediaCatalogReadRepository {
    MediaCatalogPage search(SearchMediaCatalogQuery query);
    Optional<MediaCatalogView> byId(String id);
    java.util.Map<java.util.UUID,String> articleReferences(java.util.List<java.util.UUID> ids);
    MediaCatalogArticleUsagePage articleUsages(ListMediaCatalogArticleUsagesQuery query);
}
