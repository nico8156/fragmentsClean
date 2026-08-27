package com.nm.fragmentsclean.platform.eventing.contracts;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Stable primitive contract transported through SQS for Google photo enrichment. */
public record CoffeePhotosImportedIntegrationEvent(
        UUID eventId,
        UUID commandId,
        UUID coffeeId,
        List<PhotoReference> photos,
        long version,
        Instant occurredAt,
        Instant clientAt
) {
    public CoffeePhotosImportedIntegrationEvent {
        photos = photos == null ? List.of() : List.copyOf(photos);
    }

    public record PhotoReference(UUID photoId, String photoUri) {
    }
}
