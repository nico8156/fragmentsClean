package com.nm.fragmentsclean.platform.eventing.contracts;

import java.time.Instant;
import java.util.UUID;

public record CoffeePublishedIntegrationEvent(UUID eventId, UUID commandId, UUID coffeeId, int version,
                                              Instant occurredAt) { }
