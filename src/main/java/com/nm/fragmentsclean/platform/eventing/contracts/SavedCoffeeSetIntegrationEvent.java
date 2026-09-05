package com.nm.fragmentsclean.platform.eventing.contracts;

import java.time.Instant;
import java.util.UUID;

public record SavedCoffeeSetIntegrationEvent(
        UUID eventId, UUID commandId, UUID savedCoffeeId, UUID userId, UUID coffeeId,
        boolean active, long version, Instant occurredAt, Instant clientAt) { }
