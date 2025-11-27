package com.nm.fragmentsclean.userContext.businesslogic.models;

import java.util.List;

public record BadgeProgress(
        int exploration,
        int gout,
        int social,
        List<String> unlockedBadges
) {
}
