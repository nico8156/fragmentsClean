package com.nm.fragmentsclean.platform.eventing.contracts;

import java.time.Instant;
import java.util.UUID;

public record AppUserDeletionRequestedIntegrationEvent(
    UUID eventId, UUID requestId, UUID userId, UUID authUserId, long version, Instant occurredAt) {}
