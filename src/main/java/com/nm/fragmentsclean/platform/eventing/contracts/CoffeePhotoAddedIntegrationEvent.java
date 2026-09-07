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
        boolean cover,
        int sortOrder,
        int version,
        Instant occurredAt,
        Instant clientAt
) {
	public CoffeePhotoAddedIntegrationEvent(UUID eventId, UUID commandId, UUID coffeeId, UUID photoId,
			String photoUri, int version, Instant occurredAt, Instant clientAt) {
		this(eventId, commandId, coffeeId, photoId, photoUri, false, 0, version, occurredAt, clientAt);
	}
}
