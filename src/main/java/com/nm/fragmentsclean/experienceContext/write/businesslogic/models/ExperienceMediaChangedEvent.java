package com.nm.fragmentsclean.experienceContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record ExperienceMediaChangedEvent(UUID eventId, UUID commandId, UUID mediaId,
    UUID experienceId, UUID coffeeId, UUID userId, ExperienceMediaStatus status, String objectKey,
    String contentType, long size, Integer width, Integer height, String reason, long version,
    Instant occurredAt, Instant clientAt) implements DomainEvent {}
