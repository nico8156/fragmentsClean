package com.nm.fragmentsclean.authContext.adapters.primary.springboot.dto;

public record UserPreferencesDto(
        String locale,
        Boolean marketingOptIn,
        Boolean pushOptIn,
        String theme,
        BadgeProgressDto badgeProgress
) {
}
