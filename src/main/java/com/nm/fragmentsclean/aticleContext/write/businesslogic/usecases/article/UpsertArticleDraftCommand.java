package com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleRevisionDraft;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.Command;

import java.time.Instant;
import java.util.UUID;

public record UpsertArticleDraftCommand(
        UUID commandId,
        Instant clientAt,
        UUID articleId,
        UUID revisionId,
        String slug,
        String locale,
        UUID authorId,
        String authorName,
        ArticleRevisionDraft draft
) implements Command { }
