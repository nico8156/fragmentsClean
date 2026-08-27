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
	public CoffeeCreatedIntegrationEvent(UUID eventId, UUID commandId, UUID coffeeId, String googlePlaceId,
			String name, String addressLine1, String city, String postalCode, String country, int version,
			Instant occurredAt) {
		this(eventId, commandId, coffeeId, googlePlaceId, name, addressLine1, city, postalCode, country,
				"PUBLISHED", version, occurredAt);
	}
}
