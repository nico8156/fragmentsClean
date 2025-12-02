package com.nm.fragmentsclean.socialContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.Command;

import java.time.Instant;
import java.util.UUID;

public record UpdateCommentCommand(
        UUID commandId,
        UUID commentId,
        String newBody,
        Instant clientAt
) implements Command {
}
