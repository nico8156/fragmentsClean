package com.nm.fragmentsclean.authContext.businesslogic.models;

public record UserPreferencesSnapshot(
        String locale,              // "fr-FR" / "en-US"
        Boolean marketingOptIn,
        Boolean pushOptIn,
        String theme,               // "light" | "dark" | "system"
        BadgeProgressSnapshot badgeProgress
) {
}
