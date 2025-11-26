package com.nm.fragmentsclean.socialContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.Command;

import java.time.Instant;
import java.util.UUID;

public record DeleteCommentCommand(
        UUID commandId,
        UUID commentId,
        Instant clientAt
)implements Command {
}
