package com.nm.fragmentsclean.platform.eventing.contracts;
import java.time.Instant; import java.util.List; import java.util.UUID;
public record CoffeePhotosArrangedIntegrationEvent(UUID eventId, UUID commandId, UUID coffeeId,
        List<Photo> photos, int version, Instant occurredAt, Instant clientAt) {
    public record Photo(UUID photoId, String photoUri, boolean cover, int sortOrder) { }
}
