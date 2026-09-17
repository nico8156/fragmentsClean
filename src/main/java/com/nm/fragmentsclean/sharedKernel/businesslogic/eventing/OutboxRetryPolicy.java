package com.nm.fragmentsclean.sharedKernel.businesslogic.eventing;

import java.time.Duration;

public final class OutboxRetryPolicy {
    private final int maxFailures;
    private final Duration baseDelay;
    private final Duration maximumDelay;

    public OutboxRetryPolicy(int maxFailures, Duration baseDelay, Duration maximumDelay) {
        if (maxFailures < 1 || baseDelay.isNegative() || baseDelay.isZero()
                || maximumDelay.compareTo(baseDelay) < 0) {
            throw new IllegalArgumentException("Invalid outbox retry policy");
        }
        this.maxFailures = maxFailures;
        this.baseDelay = baseDelay;
        this.maximumDelay = maximumDelay;
    }

    public boolean terminal(int failureCount) {
        return failureCount >= maxFailures;
    }

    public Duration delayFor(String eventId, int failureCount) {
        int exponent = Math.max(0, Math.min(failureCount - 1, 30));
        long multiplier = 1L << exponent;
        long uncapped;
        try {
            uncapped = Math.multiplyExact(baseDelay.toMillis(), multiplier);
        } catch (ArithmeticException overflow) {
            uncapped = maximumDelay.toMillis();
        }
        long capped = Math.min(uncapped, maximumDelay.toMillis());
        // Stable 80%-120% jitter keeps tests reproducible and workers consistent.
        int jitterPercent = 80 + Math.floorMod((eventId + ':' + failureCount).hashCode(), 41);
        return Duration.ofMillis(Math.max(1L, capped * jitterPercent / 100L));
    }
}
