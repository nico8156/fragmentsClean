package com.nm.fragmentsclean.authContext.adapters.primary.springboot.dto;

public record UserPreferencesDto(
        String locale,
        boolean marketingOptIn,
        boolean pushOptIn,
        String theme,
        BadgeProgressDto badgeProgress
) {
}
