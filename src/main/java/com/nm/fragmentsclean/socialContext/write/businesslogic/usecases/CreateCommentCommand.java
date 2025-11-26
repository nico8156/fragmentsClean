package com.nm.fragmentsclean.socialContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.Command;

import java.time.Instant;
import java.util.UUID;

public record CreateCommentCommand(
        UUID commandId,
        UUID commentId,  // correspond à tempId côté front, mappé en UUID
        UUID authorId,
        UUID targetId,
        UUID parentId,
        String body,
        Instant clientAt
) implements Command {
}
