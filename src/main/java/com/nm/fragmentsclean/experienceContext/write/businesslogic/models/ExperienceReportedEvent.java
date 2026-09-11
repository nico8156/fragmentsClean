package com.nm.fragmentsclean.experienceContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record ExperienceReportedEvent(
        UUID eventId, UUID commandId, UUID reportId, UUID experienceId, UUID coffeeId,
        UUID authorId, UUID reporterId, ExperienceReportReason reason, String details,
        ExperienceReportStatus status, long version, Instant occurredAt,
        Instant clientAt) implements DomainEvent { }
