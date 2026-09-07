package com.nm.fragmentsclean.coffeeContext.write.businessLogic.models;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;

public record CoffeePhotosArrangedEvent(UUID eventId, UUID commandId, CoffeeId coffeeId,
        List<ArrangedPhoto> photos, int version, Instant occurredAt, Instant clientAt) implements DomainEvent {
    public CoffeePhotosArrangedEvent { photos = List.copyOf(photos); }
    public record ArrangedPhoto(UUID photoId, String photoUri, boolean cover, int sortOrder) { }
}
