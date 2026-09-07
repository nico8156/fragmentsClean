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
		List<OpeningPeriod> periods,
		List<String> weekdayDescriptions,
		long version,
		Instant occurredAt,
		Instant clientAt
) implements DomainEvent {
	public CoffeeOpeningHoursImportedEvent {
		periods = periods == null ? List.of() : List.copyOf(periods);
		weekdayDescriptions = weekdayDescriptions == null ? List.of() : List.copyOf(weekdayDescriptions);
	}
	public CoffeeOpeningHoursImportedEvent(UUID eventId, UUID commandId, CoffeeId coffeeId, GooglePlaceId googlePlaceId,
			List<String> weekdayDescriptions, long version, Instant occurredAt, Instant clientAt) {
		this(eventId, commandId, coffeeId, googlePlaceId, List.of(), weekdayDescriptions, version, occurredAt, clientAt);
	}
	public record OpeningPeriod(int dayCode, int startMinute, int endMinute) { }

	@Override
	public UUID eventId() {
		return eventId;
	}

	@Override
	public Instant occurredAt() {
		return occurredAt;
	}
}
