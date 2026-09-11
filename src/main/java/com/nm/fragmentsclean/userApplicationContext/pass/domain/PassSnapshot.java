package com.nm.fragmentsclean.userApplicationContext.pass.domain;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record PassSnapshot(
        UUID userId,
        int policyVersion,
        PassCounters counters,
        Set<PassLevel> acquiredLevels,
        PassLevel currentLevel,
        List<PassLevelView> levels,
        long version,
        Instant updatedAt) {
    public PassSnapshot {
        acquiredLevels = Set.copyOf(acquiredLevels);
        levels = List.copyOf(levels);
    }
}
