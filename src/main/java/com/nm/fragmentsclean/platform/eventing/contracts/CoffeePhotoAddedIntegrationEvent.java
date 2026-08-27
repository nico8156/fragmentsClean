package com.nm.fragmentsclean.platform.eventing.contracts;

import java.time.Instant;
import java.util.UUID;

/** Stable primitive contract transported through SQS for an admin photo upload. */
public record CoffeePhotoAddedIntegrationEvent(
        UUID eventId,
        UUID commandId,
        UUID coffeeId,
        UUID photoId,
        String photoUri,
        int version,
        Instant occurredAt,
        Instant clientAt
) {
}
