package com.nm.fragmentsclean.authContext.write.adapters.primary.springboot.dto;

import java.util.List;
import java.util.Map;

public record AppUserDto(
        String id,
        String createdAt,
        String updatedAt,
        String displayName,
        String avatarUrl,
        String bio,
        List<LinkedIdentityDto> identities,
        List<String> roles,
        Map<String, Boolean> flags,
        UserPreferencesDto preferences,
        List<String> likedCoffeeIds,
        long version
) {
}
