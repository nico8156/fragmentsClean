package com.nm.fragmentsclean.userContext.businesslogic.models;

import java.util.List;

public record UserPreferences(
        String locale,
        Boolean marketingOptIn,
        Boolean pushOptIn,
        String theme,
        BadgeProgress badgeProgress
) {

    public static UserPreferences defaultForLocale(String locale) {
        return new UserPreferences(
                locale != null ? locale : "fr-FR",
                false,
                true,
                "system",
                new BadgeProgress(0, 0, 0, List.of())
        );
    }
}