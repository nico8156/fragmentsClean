package com.nm.fragmentsclean.coffeeContext.write.businessLogic.models;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;

public record CoffeeOpeningHoursUpdatedEvent(UUID eventId, UUID commandId, CoffeeId coffeeId,
        List<OpeningPeriod> periods, int version, Instant occurredAt, Instant clientAt) implements DomainEvent {
    public CoffeeOpeningHoursUpdatedEvent { periods = periods == null ? List.of() : List.copyOf(periods); }
    public record OpeningPeriod(int dayCode, int startMinute, int endMinute) { }
}
