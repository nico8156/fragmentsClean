package com.nm.fragmentsclean.experienceContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperienceModerationStatus;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.AuthenticatedCommand;
import java.time.Instant;
import java.util.UUID;

public record ModerateExperienceCommand(UUID commandId, UUID actionId, UUID reportId,
                                        UUID experienceId, UUID operatorId,
                                        ExperienceModerationStatus decision, String reason,
                                        Instant clientAt) implements AuthenticatedCommand {
    @Override public UUID receiptCommandId() { return commandId; }
    @Override public UUID requesterId() { return operatorId; }
    @Override public String receiptType() { return "experience.moderate.v1"; }
}
