package com.nm.fragmentsclean.authContext.write.businesslogic.models;

public record LinkedIdentitySnapshot(
        String id,
        String provider,
        String providerUserId,
        String email,
        String createdAt,
        String lastAuthAt
) {
}
