package com.nm.fragmentsclean.platform.eventing.contracts;

import java.time.Instant;
import java.util.UUID;

public record CoffeeLifecycleIntegrationEvent(
		UUID eventId,
		UUID commandId,
		UUID coffeeId,
		int version,
		Instant occurredAt
) {
}
