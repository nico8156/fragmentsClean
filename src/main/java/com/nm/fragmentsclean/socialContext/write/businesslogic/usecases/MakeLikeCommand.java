package com.nm.fragmentsclean.socialContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.AuthenticatedCommand;

import java.time.Instant;
import java.util.UUID;

public record MakeLikeCommand(
        String commandId,     // idempotence côté back
        UUID likeId,        // ID d’agrégat fourni par le front
        UUID userId,
        UUID targetId,
        boolean value,      // true = LIKE, false = UNLIKE
        Instant clientAt
) implements AuthenticatedCommand {
    @Override
    public UUID receiptCommandId() {
        return UUID.fromString(commandId);
    }

    @Override
    public UUID requesterId() {
        return userId;
    }

    @Override
    public String receiptType() {
        return "social.like.set.v1";
    }
}
