package com.nm.fragmentsclean.userApplicationContext.read.projections;

import java.time.Instant;
import java.util.UUID;

public record AppUserProfileView(
    UUID userId,
    String displayName,
    String avatarUrl,
    Instant createdAt,
    Instant updatedAt,
    long version) {}
