package com.nm.fragmentsclean.platform.eventing.contracts;

import java.time.Instant;
import java.util.UUID;

public record CoffeeCreatedIntegrationEvent(
		UUID eventId,
		UUID commandId,
		UUID coffeeId,
		String name,
		String addressLine1,
		String city,
		String postalCode,
		String country,
		int version,
		Instant occurredAt
) {
}
