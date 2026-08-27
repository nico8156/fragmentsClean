package com.nm.fragmentsclean.platform.eventing.contracts;

import java.time.Instant;
import java.util.UUID;

public record CoffeeCreatedIntegrationEvent(
		UUID eventId,
		UUID commandId,
		UUID coffeeId,
		String googlePlaceId,
		String name,
		String addressLine1,
		String city,
		String postalCode,
		String country,
		String publicationStatus,
		int version,
		Instant occurredAt
) {
}
