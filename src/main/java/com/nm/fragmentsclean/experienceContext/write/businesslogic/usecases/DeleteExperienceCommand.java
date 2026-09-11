package com.nm.fragmentsclean.experienceContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.AuthenticatedCommand;
import java.time.Instant;
import java.util.UUID;

public record DeleteExperienceCommand(UUID commandId, UUID experienceId, UUID userId,
                                      Instant clientAt) implements AuthenticatedCommand {
    @Override public UUID receiptCommandId() { return commandId; }
    @Override public UUID requesterId() { return userId; }
    @Override public String receiptType() { return "experience.delete.v1"; }
}
