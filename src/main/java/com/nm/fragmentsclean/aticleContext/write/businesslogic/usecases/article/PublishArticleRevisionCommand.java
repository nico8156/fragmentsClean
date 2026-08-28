package com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.Command;

import java.time.Instant;
import java.util.UUID;

public record PublishArticleRevisionCommand(
        UUID commandId,
        Instant clientAt,
        UUID articleId,
        UUID expectedRevisionId
) implements Command {
}
