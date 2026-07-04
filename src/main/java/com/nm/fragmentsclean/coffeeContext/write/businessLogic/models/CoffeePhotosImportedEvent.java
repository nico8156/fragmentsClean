package com.nm.fragmentsclean.coffeeContext.write.businessLogic.models;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.GooglePlaceId;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CoffeePhotosImportedEvent(
		UUID eventId,
		UUID commandId,
		CoffeeId coffeeId,
		GooglePlaceId googlePlaceId,
		List<ImportedCoffeePhoto> photos,
		long version,
		Instant occurredAt,
		Instant clientAt
) implements DomainEvent {
	public CoffeePhotosImportedEvent {
		photos = photos == null ? List.of() : List.copyOf(photos);
	}

	@Override
	public UUID eventId() {
		return eventId;
	}

	@Override
	public UUID commandId() {
		return commandId;
	}

	@Override
	public Instant occurredAt() {
		return occurredAt;
	}

	@Override
	public Instant clientAt() {
		return clientAt;
	}
}
