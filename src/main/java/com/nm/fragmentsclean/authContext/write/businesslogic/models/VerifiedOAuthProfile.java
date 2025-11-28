package com.nm.fragmentsclean.authContext.write.businesslogic.models;

public record VerifiedOAuthProfile(
        String provider,        // "google"
        String providerUserId,  // sub
        String email,
        boolean emailVerified,
        String displayName,
        String avatarUrl,
        String locale
) {
}
