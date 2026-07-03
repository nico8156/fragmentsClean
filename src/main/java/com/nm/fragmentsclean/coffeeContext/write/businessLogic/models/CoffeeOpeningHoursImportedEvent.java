package com.nm.fragmentsclean.coffeeContext.write.businessLogic.models;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.GooglePlaceId;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CoffeeOpeningHoursImportedEvent(
		UUID eventId,
		UUID commandId,
		CoffeeId coffeeId,
		GooglePlaceId googlePlaceId,
		List<String> weekdayDescriptions,
		long version,
		Instant occurredAt,
		Instant clientAt
) implements DomainEvent {
	public CoffeeOpeningHoursImportedEvent {
		weekdayDescriptions = weekdayDescriptions == null ? List.of() : List.copyOf(weekdayDescriptions);
	}

	@Override
	public UUID eventId() {
		return eventId;
	}

	@Override
	public Instant occurredAt() {
		return occurredAt;
	}
}
