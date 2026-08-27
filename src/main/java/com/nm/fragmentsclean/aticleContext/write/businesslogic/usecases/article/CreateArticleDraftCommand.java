package com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleContent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.Command;

import java.time.Instant;
import java.util.UUID;

public record CreateArticleDraftCommand(
        UUID commandId,
        Instant clientAt,
        UUID articleId,
        UUID revisionId,
        String slug,
        String locale,
        UUID authorId,
        String authorName,
        ArticleContent content
) implements Command {
}
