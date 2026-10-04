package com.nm.fragmentsclean.coffeeContext.write.businessLogic.models;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import java.time.Instant;
import java.util.*;
/** Source inventory for the admin catalogue only; never a public photo gallery. */
public record CoffeeMediaCatalogSnapshotEvent(UUID eventId,UUID commandId,CoffeeId coffeeId,
        List<CoffeePhotosArrangedEvent.ArrangedPhoto> photos,List<RetiredPhoto> retiredPhotos,
        int version,Instant occurredAt,Instant clientAt) implements DomainEvent {
    public record RetiredPhoto(UUID photoId,Instant retiredAt) {}
    public CoffeeMediaCatalogSnapshotEvent {photos=List.copyOf(photos);retiredPhotos=List.copyOf(retiredPhotos);}
    public CoffeeMediaCatalogSnapshotEvent(UUID eventId,UUID commandId,CoffeeId coffeeId,List<CoffeePhotosArrangedEvent.ArrangedPhoto> photos,int version,Instant occurredAt,Instant clientAt){this(eventId,commandId,coffeeId,photos,List.of(),version,occurredAt,clientAt);}
}
