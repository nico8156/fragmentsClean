package com.nm.fragmentsclean.platform.eventing.contracts;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CoffeeOpeningHoursImportedIntegrationEvent(
        UUID eventId, UUID commandId, UUID coffeeId, String googlePlaceId,
        List<OpeningPeriod> periods, List<String> weekdayDescriptions, long version, Instant occurredAt, Instant clientAt) {
    public CoffeeOpeningHoursImportedIntegrationEvent {
        periods = periods == null ? List.of() : List.copyOf(periods);
        weekdayDescriptions = weekdayDescriptions == null ? List.of() : List.copyOf(weekdayDescriptions);
    }
    public CoffeeOpeningHoursImportedIntegrationEvent(UUID eventId, UUID commandId, UUID coffeeId, String googlePlaceId,
            List<String> weekdayDescriptions, long version, Instant occurredAt, Instant clientAt) {
        this(eventId, commandId, coffeeId, googlePlaceId, List.of(), weekdayDescriptions, version, occurredAt, clientAt);
    }
    public record OpeningPeriod(int dayCode, int startMinute, int endMinute) { }
}
