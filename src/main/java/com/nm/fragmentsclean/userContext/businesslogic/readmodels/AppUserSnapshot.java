package com.nm.fragmentsclean.userContext.businesslogic.readmodels;

import java.util.List;
import java.util.Map;

public record AppUserSnapshot(
        String id,
        String createdAt,
        String updatedAt,
        String displayName,
        String avatarUrl,
        String bio,
        List<LinkedIdentitySnapshot> identities,
        List<String> roles,
        Map<String, Boolean> flags,
        UserPreferencesSnapshot preferences,
        List<String> likedCoffeeIds,
        long version
) {
}
