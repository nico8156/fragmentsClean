package com.nm.fragmentsclean.authContext.businesslogic.models;

import java.util.List;

public record BadgeProgressSnapshot(
        int exploration,
        int gout,
        int social,
        List<String> unlockedBadges
) {
}
