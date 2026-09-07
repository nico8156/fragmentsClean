package com.nm.fragmentsclean.platform.eventing.contracts;

import java.time.Instant;
import java.util.UUID;

public record CoffeeDetailsEditedIntegrationEvent(UUID eventId, UUID commandId, UUID coffeeId, int version,
        Instant occurredAt, Instant clientAt) { }
