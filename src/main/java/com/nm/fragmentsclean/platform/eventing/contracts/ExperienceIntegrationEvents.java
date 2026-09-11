package com.nm.fragmentsclean.platform.eventing.contracts;

import java.time.Instant;
import java.util.UUID;

/** Stable primitive contracts reserved for the Experience producer introduced in lot 05. */
public final class ExperienceIntegrationEvents {
    private ExperienceIntegrationEvents() { }

    public record LifecycleChanged(
            UUID eventId,
            UUID experienceId,
            UUID userId,
            UUID coffeeId,
            String publicationStatus,
            String moderationStatus,
            String reason,
            long version,
            Instant occurredAt) { }
}
