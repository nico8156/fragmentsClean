package com.nm.fragmentsclean.platform.eventing.contracts;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CoffeeOpeningHoursUpdatedIntegrationEvent(UUID eventId, UUID commandId, UUID coffeeId,
        List<OpeningPeriod> periods, int version, Instant occurredAt, Instant clientAt) {
    public CoffeeOpeningHoursUpdatedIntegrationEvent { periods = periods == null ? List.of() : List.copyOf(periods); }
    public record OpeningPeriod(int dayCode, int startMinute, int endMinute) { }
}
