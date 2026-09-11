package com.nm.fragmentsclean.userApplicationContext.pass.domain;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PassProgressPolicy {
    public static final int VERSION = 2;
    private static final Map<PassLevel, PassRequirements> REQUIREMENTS = requirements();

    private PassProgressPolicy() { }

    public static Set<PassLevel> eligibleLevels(PassCounters counters) {
        var eligible = EnumSet.noneOf(PassLevel.class);
        for (PassLevel level : PassLevel.values()) {
            PassRequirements required = REQUIREMENTS.get(level);
            if (counters.publishedExperiences() >= required.publishedExperiences()
                    && counters.distinctExperiencedCoffees() >= required.distinctExperiencedCoffees()
                    && counters.validatedTickets() >= required.validatedTickets()) {
                eligible.add(level);
            }
        }
        return Set.copyOf(eligible);
    }

    public static PassSnapshot evaluate(java.util.UUID userId, PassCounters counters,
                                        Set<PassLevel> acquiredLevels, long version,
                                        java.time.Instant updatedAt) {
        var acquired = acquiredLevels.isEmpty()
                ? EnumSet.noneOf(PassLevel.class)
                : EnumSet.copyOf(acquiredLevels);
        List<PassLevelView> levels = new ArrayList<>();
        boolean foundCurrent = false;
        for (PassLevel level : PassLevel.values()) {
            PassLevelStatus status;
            if (acquired.contains(level)) status = PassLevelStatus.COMPLETED;
            else if (!foundCurrent) {
                status = PassLevelStatus.IN_PROGRESS;
                foundCurrent = true;
            } else status = PassLevelStatus.LOCKED;
            levels.add(new PassLevelView(level, status, REQUIREMENTS.get(level)));
        }
        PassLevel current = levels.stream()
                .filter(level -> level.status() == PassLevelStatus.IN_PROGRESS)
                .map(PassLevelView::level)
                .findFirst()
                .orElse(PassLevel.FRAGMENTS_MASTER);
        return new PassSnapshot(userId, VERSION, counters, acquired, current, levels,
                version, updatedAt);
    }

    public static PassRequirements requirementsFor(PassLevel level) {
        return REQUIREMENTS.get(level);
    }

    public static boolean countsExperience(String publicationStatus, String moderationStatus) {
        return "PUBLISHED".equals(publicationStatus) && "VISIBLE".equals(moderationStatus);
    }

    public static boolean revokesAcquiredLevels(boolean active, String reason) {
        if (active || reason == null) return false;
        return "FRAUD_CONFIRMED".equals(reason) || "MODERATION_HIDDEN".equals(reason);
    }

    private static Map<PassLevel, PassRequirements> requirements() {
        var requirements = new EnumMap<PassLevel, PassRequirements>(PassLevel.class);
        requirements.put(PassLevel.COFFEE_TASTER, new PassRequirements(1, 1, 0));
        requirements.put(PassLevel.URBAN_EXPLORER, new PassRequirements(3, 3, 0));
        requirements.put(PassLevel.SOCIAL_BEAN, new PassRequirements(5, 3, 1));
        requirements.put(PassLevel.FRAGMENTS_MASTER, new PassRequirements(10, 5, 3));
        return Map.copyOf(requirements);
    }
}
