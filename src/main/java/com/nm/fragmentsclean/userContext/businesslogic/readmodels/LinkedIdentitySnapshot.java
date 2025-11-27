package com.nm.fragmentsclean.userContext.businesslogic.readmodels;

public record LinkedIdentitySnapshot(
        String id,
        String provider,
        String providerUserId,
        String email,
        String createdAt,
        String lastAuthAt
) {
}
