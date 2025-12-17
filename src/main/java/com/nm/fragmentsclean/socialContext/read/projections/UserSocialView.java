package com.nm.fragmentsclean.socialContext.read.projections;

import java.time.Instant;
import java.util.UUID;

public record UserSocialView(
        UUID userId,
        String displayName,
        String avatarUrl,
        Instant updatedAt,
        long version
) {
}
