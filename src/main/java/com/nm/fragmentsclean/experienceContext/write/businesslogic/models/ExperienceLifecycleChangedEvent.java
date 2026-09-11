package com.nm.fragmentsclean.experienceContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record ExperienceLifecycleChangedEvent(
        UUID eventId, UUID experienceId, UUID userId, UUID coffeeId,
        ExperiencePublicationStatus publicationStatus,
        ExperienceModerationStatus moderationStatus, String reason,
        long version, Instant occurredAt) implements DomainEvent { }
