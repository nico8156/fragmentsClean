package com.nm.fragmentsclean.platform.eventing.contracts;

import java.time.Instant;
import java.util.UUID;

public record AuthUserLoggedInIntegrationEvent(
        UUID eventId, UUID authUserId, String provider, String providerUserId, Instant occurredAt) { }
