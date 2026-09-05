package com.nm.fragmentsclean.platform.eventing.contracts;

import java.time.Instant;
import java.util.UUID;

public record CoffeePhotoDeletedIntegrationEvent(
        UUID eventId, UUID commandId, UUID coffeeId, UUID photoId,
        int version, Instant occurredAt, Instant clientAt) { }
