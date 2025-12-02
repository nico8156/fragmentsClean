package com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.Command;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateArticleCommand(
        UUID commandId,
        Instant clientAt,
        UUID articleId,

        String slug,
        String locale,

        UUID authorId,
        String authorName,

        String title,
        String intro,
        String blocksJson,
        String conclusion,
        String coverUrl,
        Integer coverWidth,
        Integer coverHeight,
        String coverAlt,
        List<String> tags,
        Integer readingTimeMin,
        List<UUID> coffeeIds
) implements Command {
}
