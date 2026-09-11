package com.nm.fragmentsclean.socialContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.AuthenticatedCommand;

import java.time.Instant;
import java.util.UUID;

public record UpdateCommentCommand(
        UUID commandId,
        UUID commentId,
        UUID userId,
        String newBody,
        Instant clientAt
) implements AuthenticatedCommand {
    @Override
    public UUID receiptCommandId() {
        return commandId;
    }

    @Override
    public UUID requesterId() {
        return userId;
    }

    @Override
    public String receiptType() {
        return "social.comment.update.v1";
    }
}
