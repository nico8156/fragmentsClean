package com.nm.fragmentsclean.experienceContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record ExperienceSnapshotChangedEvent(
        UUID eventId, UUID commandId, UUID experienceId, UUID userId, UUID coffeeId,
        String message, ExperiencePublicationStatus publicationStatus,
        ExperienceModerationStatus moderationStatus, String reason, long version,
        Instant createdAt, Instant updatedAt, Instant deletedAt, Instant occurredAt,
        Instant clientAt) implements DomainEvent { }
