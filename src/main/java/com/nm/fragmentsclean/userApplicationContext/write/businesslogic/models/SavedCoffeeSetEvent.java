package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record SavedCoffeeSetEvent(
		UUID eventId,
		UUID commandId,
		UUID savedCoffeeId,
		UUID userId,
		UUID coffeeId,
		boolean active,
		long version,
		Instant occurredAt,
		Instant clientAt
) implements DomainEvent {
}
