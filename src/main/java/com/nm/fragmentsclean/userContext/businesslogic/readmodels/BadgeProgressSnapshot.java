package com.nm.fragmentsclean.userContext.businesslogic.readmodels;

import java.util.List;

public record BadgeProgressSnapshot(
        int exploration,
        int gout,
        int social,
        List<String> unlockedBadges
) {
}
