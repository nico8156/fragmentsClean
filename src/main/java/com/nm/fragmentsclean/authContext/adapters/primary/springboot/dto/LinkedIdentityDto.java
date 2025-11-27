package com.nm.fragmentsclean.authContext.adapters.primary.springboot.dto;

public record LinkedIdentityDto(
        String id,
        String provider,
        String providerUserId,
        String email,
        String createdAt,
        String lastAuthAt
) {
}


