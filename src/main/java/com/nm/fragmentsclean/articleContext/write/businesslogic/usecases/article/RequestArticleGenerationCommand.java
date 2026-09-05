package com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article;

import com.nm.fragmentsclean.articleContext.write.businesslogic.processManagers.ArticleAuthoringTrigger;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.Command;
import java.time.Instant;
import java.util.UUID;

public record RequestArticleGenerationCommand(
        UUID commandId, Instant clientAt, UUID sagaId, UUID articleId, UUID revisionId,
        String theme, String slug, String locale, UUID authorId, String authorName,
        ArticleAuthoringTrigger trigger
) implements Command { }
