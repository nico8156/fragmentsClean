package com.nm.fragmentsclean.platform.eventing.contracts;

import java.time.Instant;
import java.util.UUID;

public record AccountDataErasedIntegrationEvent(
    UUID eventId, UUID requestId, UUID userId, String context, Instant occurredAt) {}
