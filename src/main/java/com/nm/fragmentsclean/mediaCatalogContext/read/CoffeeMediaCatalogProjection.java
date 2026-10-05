package com.nm.fragmentsclean.mediaCatalogContext.read;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
/** Local catalogue effects, never a producer repository. */
public interface CoffeeMediaCatalogProjection {
    void applyCoffee(UUID coffeeId,List<MediaCatalogEntry> entries,long version,Instant at,boolean complete,boolean deleted);
}
