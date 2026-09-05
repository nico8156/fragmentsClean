package com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article;

import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleRevisionDraft;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.Command;

import java.time.Instant;
import java.util.UUID;

public record EditArticleDraftCommand(
        UUID commandId,
        Instant clientAt,
        UUID articleId,
        UUID revisionId,
        ArticleRevisionDraft draft
) implements Command { }
