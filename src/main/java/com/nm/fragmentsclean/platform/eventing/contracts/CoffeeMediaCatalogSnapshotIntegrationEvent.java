package com.nm.fragmentsclean.platform.eventing.contracts;
import java.time.Instant;
import java.util.*;
/** coffee.media_catalog_snapshot v2: active gallery plus retained source references. */
public record CoffeeMediaCatalogSnapshotIntegrationEvent(UUID eventId,UUID commandId,UUID coffeeId,
        List<CoffeePhotosArrangedIntegrationEvent.Photo> photos,List<RetiredPhoto> retiredPhotos,
        int version,Instant occurredAt,Instant clientAt) {
    public record RetiredPhoto(UUID photoId,Instant retiredAt) {}
    public CoffeeMediaCatalogSnapshotIntegrationEvent {photos=List.copyOf(photos);retiredPhotos=List.copyOf(retiredPhotos);}
}
