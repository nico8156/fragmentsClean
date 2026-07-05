package com.nm.fragmentsclean.coffeeContext.write.businessLogic.models;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;

public record CoffeePhotoAddedEvent(
		UUID eventId,
		UUID commandId,
		CoffeeId coffeeId,
		ImportedCoffeePhoto photo,
		int version,
		Instant occurredAt,
		Instant clientAt
) implements DomainEvent {
	public CoffeePhotoAddedEvent {
		Objects.requireNonNull(eventId, "eventId is required");
		Objects.requireNonNull(commandId, "commandId is required");
		Objects.requireNonNull(coffeeId, "coffeeId is required");
		Objects.requireNonNull(photo, "photo is required");
		Objects.requireNonNull(occurredAt, "occurredAt is required");
	}
}
