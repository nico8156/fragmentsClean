package com.nm.fragmentsclean.socialContext.read.projections;

import java.time.Instant;
import java.util.UUID;

public record BlockedUserView(UUID blockId, UUID userId, String displayName, String avatarUrl,
                              Instant blockedAt, long version) { }
