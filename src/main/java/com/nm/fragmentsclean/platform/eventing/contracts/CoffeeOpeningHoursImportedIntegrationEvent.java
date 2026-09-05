package com.nm.fragmentsclean.platform.eventing.contracts;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CoffeeOpeningHoursImportedIntegrationEvent(
        UUID eventId, UUID commandId, UUID coffeeId, String googlePlaceId,
        List<String> weekdayDescriptions, long version, Instant occurredAt, Instant clientAt) {
    public CoffeeOpeningHoursImportedIntegrationEvent {
        weekdayDescriptions = weekdayDescriptions == null ? List.of() : List.copyOf(weekdayDescriptions);
    }
}
