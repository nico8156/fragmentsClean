package com.nm.fragmentsclean.platform.eventing.contracts;

import java.time.Instant;
import java.util.UUID;

/** Stable primitive contracts published and consumed by the Experience vertical. */
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

    public record SnapshotChanged(
            UUID eventId, UUID commandId, UUID experienceId, UUID userId, UUID coffeeId,
            String message, String publicationStatus, String moderationStatus, String reason,
            long version, Instant createdAt, Instant updatedAt, Instant deletedAt,
            Instant occurredAt, Instant clientAt) { }

    public record Reported(
            UUID eventId, UUID commandId, UUID reportId, UUID experienceId, UUID coffeeId,
            UUID authorId, UUID reporterId, String reason, String details, String status,
            long version, Instant occurredAt, Instant clientAt) { }

    public record Moderated(
            UUID eventId, UUID commandId, UUID actionId, UUID reportId, UUID experienceId,
            UUID coffeeId, UUID authorId, UUID operatorId, String moderationStatus,
            String reportStatus, String reason, long version, Instant occurredAt,
            Instant clientAt) { }
}
