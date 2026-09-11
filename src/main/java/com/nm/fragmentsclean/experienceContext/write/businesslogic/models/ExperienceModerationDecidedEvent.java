package com.nm.fragmentsclean.experienceContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record ExperienceModerationDecidedEvent(
        UUID eventId, UUID commandId, UUID actionId, UUID reportId, UUID experienceId,
        UUID coffeeId, UUID authorId, UUID operatorId,
        ExperienceModerationStatus moderationStatus, ExperienceReportStatus reportStatus,
        String reason, long version, Instant occurredAt, Instant clientAt) implements DomainEvent { }
