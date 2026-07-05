package com.nm.fragmentsclean.coffeeContext.write.businessLogic.models;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.PhotoId;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;

public record CoffeePhotoDeletedEvent(
		UUID eventId,
		UUID commandId,
		CoffeeId coffeeId,
		PhotoId photoId,
		int version,
		Instant occurredAt,
		Instant clientAt
) implements DomainEvent {
	public CoffeePhotoDeletedEvent {
		Objects.requireNonNull(eventId, "eventId is required");
		Objects.requireNonNull(commandId, "commandId is required");
		Objects.requireNonNull(coffeeId, "coffeeId is required");
		Objects.requireNonNull(photoId, "photoId is required");
		Objects.requireNonNull(occurredAt, "occurredAt is required");
	}
}
