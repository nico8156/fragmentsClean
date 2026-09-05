package com.nm.fragmentsclean.platform.eventing.contracts;

import java.time.Instant;
import java.util.UUID;

public record SocialLikeSetIntegrationEvent(
        UUID eventId, String commandId, UUID likeId, UUID userId, UUID targetId,
        boolean active, long count, long version, Instant occurredAt, Instant clientAt) { }
