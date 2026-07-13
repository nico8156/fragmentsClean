package com.nm.fragmentsclean.ticketContext.read.pass;

import java.util.List;

public final class PassProgressPolicy {

    private PassProgressPolicy() {
    }

    public static List<PassLevelView> levelsFor(PassCountersView counters) {
        boolean coffeeCompleted = counters.validatedTickets() >= 3;
        boolean urbanCompleted = coffeeCompleted
                && counters.validatedTickets() >= 5
                && counters.publishedComments() >= 3;
        boolean socialCompleted = urbanCompleted
                && counters.validatedTickets() >= 10
                && counters.publishedComments() >= 5
                && counters.confirmedLikes() >= 5;

        return List.of(
                new PassLevelView(
                        PassLevel.COFFEE_TASTER,
                        status(true, coffeeCompleted, hasProgress(counters.validatedTickets())),
                        new PassRequirementsView(3, null, null),
                        List.of("SCAN")),
                new PassLevelView(
                        PassLevel.URBAN_EXPLORER,
                        status(coffeeCompleted, urbanCompleted, hasProgress(counters.validatedTickets(), counters.publishedComments())),
                        new PassRequirementsView(5, 3, null),
                        List.of("COMMENT")),
                new PassLevelView(
                        PassLevel.SOCIAL_BEAN,
                        status(urbanCompleted, socialCompleted, hasProgress(counters.validatedTickets(), counters.publishedComments(), counters.confirmedLikes())),
                        new PassRequirementsView(10, 5, 5),
                        List.of("LIKE")),
                new PassLevelView(
                        PassLevel.FRAGMENTS_MASTER,
                        socialCompleted ? PassLevelStatus.COMPLETED : PassLevelStatus.LOCKED,
                        new PassRequirementsView(null, null, null),
                        List.of()));
    }

    public static PassLevel currentLevel(PassCountersView counters) {
        List<PassLevelView> levels = levelsFor(counters);
        return levels.stream()
                .filter(level -> level.status() == PassLevelStatus.IN_PROGRESS)
                .findFirst()
                .or(() -> levels.stream().filter(level -> level.level() == PassLevel.FRAGMENTS_MASTER
                        && level.status() == PassLevelStatus.COMPLETED).findFirst())
                .orElse(levels.get(0))
                .level();
    }

    public static List<String> rightsFor(PassCountersView counters) {
        boolean urbanCompleted = counters.validatedTickets() >= 5 && counters.publishedComments() >= 3;
        boolean socialCompleted = counters.validatedTickets() >= 10
                && counters.publishedComments() >= 5
                && counters.confirmedLikes() >= 5;

        if (socialCompleted) {
            return List.of("COMMENT", "LIKE");
        }
        if (urbanCompleted) {
            return List.of("COMMENT");
        }
        return List.of();
    }

    private static PassLevelStatus status(boolean accessible, boolean completed, boolean hasProgress) {
        if (completed) return PassLevelStatus.COMPLETED;
        if (!accessible) return PassLevelStatus.LOCKED;
        return hasProgress ? PassLevelStatus.IN_PROGRESS : PassLevelStatus.IN_PROGRESS;
    }

    private static boolean hasProgress(int... values) {
        for (int value : values) {
            if (value > 0) return true;
        }
        return false;
    }
}
