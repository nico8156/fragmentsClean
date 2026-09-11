package com.nm.fragmentsclean.experienceContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.AuthenticatedCommand;
import java.time.Instant;
import java.util.UUID;

public record ConfirmExperienceMediaCommand(UUID commandId, UUID experienceId, UUID mediaId,
    UUID userId, String objectKey, String contentType, long size, int width, int height,
    String sha256, Instant clientAt) implements AuthenticatedCommand {
  @Override public UUID receiptCommandId() { return commandId; }
  @Override public UUID requesterId() { return userId; }
  @Override public String receiptType() { return "experience.media.confirm.v1"; }
}
