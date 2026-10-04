package com.nm.fragmentsclean.coffeeContext.write.businessLogic.models;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import java.time.Instant;
import java.util.*;
/** Reconciliation fact for the admin index only; never a public gallery update. */
public record CoffeeMediaCatalogSnapshotEvent(UUID eventId,UUID commandId,CoffeeId coffeeId,List<CoffeePhotosArrangedEvent.ArrangedPhoto> photos,int version,Instant occurredAt,Instant clientAt) implements DomainEvent {
    public CoffeeMediaCatalogSnapshotEvent {photos=List.copyOf(photos);}
}
