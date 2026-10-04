package com.nm.fragmentsclean.coffeeContext.write.businessLogic.models;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import java.time.Instant;
import java.util.*;
/** Source inventory for the admin catalogue only; never a public photo gallery. */
public record CoffeeMediaCatalogSnapshotEvent(UUID eventId,UUID commandId,CoffeeId coffeeId,
        List<CoffeePhotosArrangedEvent.ArrangedPhoto> photos,List<RetiredPhoto> retiredPhotos,
        int version,Instant occurredAt,Instant clientAt) implements DomainEvent {
    public record RetiredPhoto(UUID photoId,Instant retiredAt,String status) {public RetiredPhoto(UUID photoId,Instant retiredAt){this(photoId,retiredAt,"RETIRED");}}
    public static CoffeeMediaCatalogSnapshotEvent from(Coffee coffee,List<CoffeePhotoRetirement> retained,UUID commandId,Instant now){return new CoffeeMediaCatalogSnapshotEvent(UUID.randomUUID(),commandId,coffee.coffeeId(),coffee.photos().stream().map(p->new CoffeePhotosArrangedEvent.ArrangedPhoto(p.id().value(),p.uri(),p.isCover(),p.sortOrder())).toList(),retained.stream().map(p->new RetiredPhoto(p.photo().id().value(),p.retiredAt(),p.status())).toList(),coffee.version(),now,null);}
    public CoffeeMediaCatalogSnapshotEvent {photos=List.copyOf(photos);retiredPhotos=List.copyOf(retiredPhotos);}
    public CoffeeMediaCatalogSnapshotEvent(UUID eventId,UUID commandId,CoffeeId coffeeId,List<CoffeePhotosArrangedEvent.ArrangedPhoto> photos,int version,Instant occurredAt,Instant clientAt){this(eventId,commandId,coffeeId,photos,List.of(),version,occurredAt,clientAt);}
}
