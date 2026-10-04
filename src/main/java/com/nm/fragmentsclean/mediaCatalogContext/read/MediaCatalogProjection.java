package com.nm.fragmentsclean.mediaCatalogContext.read;
import java.util.UUID;
public interface MediaCatalogProjection {
    boolean apply(MediaCatalogEntry entry);
    void erase(UUID ownerId);
}
