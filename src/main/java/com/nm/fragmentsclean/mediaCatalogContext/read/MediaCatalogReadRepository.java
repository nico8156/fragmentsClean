package com.nm.fragmentsclean.mediaCatalogContext.read;
import java.util.Optional;
public interface MediaCatalogReadRepository {
    MediaCatalogPage search(SearchMediaCatalogQuery query);
    Optional<MediaCatalogView> byId(String id);
}
