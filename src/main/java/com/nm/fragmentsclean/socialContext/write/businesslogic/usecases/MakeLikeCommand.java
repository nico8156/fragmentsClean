package com.nm.fragmentsclean.socialContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.Command;

import java.time.Instant;
import java.util.UUID;

public record MakeLikeCommand(
        UUID commandId,     // idempotence côté back
        UUID likeId,        // ID d’agrégat fourni par le front
        UUID userId,
        UUID targetId,
        boolean value,      // true = LIKE, false = UNLIKE
        Instant clientAt
) implements Command { }
