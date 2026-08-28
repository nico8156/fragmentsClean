package com.nm.fragmentsclean.adminImportContext.businessLogic.usecases;

import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.ArticleAuthoringPort;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.UuidGenerator;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import java.util.UUID;

public final class ArchiveStudioArticle {
    private final ArticleAuthoringPort authoring;
    private final UuidGenerator ids;
    private final DateTimeProvider clock;
    public ArchiveStudioArticle(ArticleAuthoringPort authoring, UuidGenerator ids, DateTimeProvider clock) {
        this.authoring = authoring; this.ids = ids; this.clock = clock;
    }
    public UUID execute(UUID articleId) {
        UUID commandId = ids.generate();
        authoring.archive(commandId, clock.now(), articleId);
        return commandId;
    }
}
