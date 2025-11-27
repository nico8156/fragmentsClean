package com.nm.fragmentsclean.authContext.adapters.primary.springboot.dto;

import java.util.List;

public record BadgeProgressDto(
        int exploration,
        int gout,
        int social,
        List<String> unlockedBadges
) {
}
