package com.nm.fragmentsclean.platform.eventing.contracts;
import java.time.Instant;
import java.util.*;
/** coffee.media_catalog_snapshot v3: retained source lifecycle including physical tombstones. */
public record CoffeeMediaCatalogSnapshotV3IntegrationEvent(UUID eventId,UUID commandId,UUID coffeeId,
        List<CoffeePhotosArrangedIntegrationEvent.Photo> photos,List<RetiredPhoto> retiredPhotos,
        int version,Instant occurredAt,Instant clientAt) {
    public record RetiredPhoto(UUID photoId,Instant retiredAt,String status) {public RetiredPhoto {if(!Set.of("RETIRED","DELETION_PENDING","DELETED").contains(status))throw new IllegalArgumentException("Unknown retired coffee media state");}}
    public CoffeeMediaCatalogSnapshotV3IntegrationEvent {photos=List.copyOf(photos);retiredPhotos=List.copyOf(retiredPhotos);}
}
