package com.nm.fragmentsclean.userContext.businesslogic.readmodels;

public record UserPreferencesSnapshot(
        String locale,
        Boolean marketingOptIn,
        Boolean pushOptIn,
        String theme,
        BadgeProgressSnapshot badgeProgress
) {
}
