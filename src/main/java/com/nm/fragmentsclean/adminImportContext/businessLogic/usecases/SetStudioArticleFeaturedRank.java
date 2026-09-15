package com.nm.fragmentsclean.adminImportContext.businessLogic.usecases;

import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.ArticleAuthoringPort;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.UuidGenerator;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import java.util.UUID;

public final class SetStudioArticleFeaturedRank {
    private final ArticleAuthoringPort authoring;
    private final UuidGenerator ids;
    private final DateTimeProvider clock;
    public SetStudioArticleFeaturedRank(ArticleAuthoringPort authoring, UuidGenerator ids, DateTimeProvider clock) {
        this.authoring = authoring; this.ids = ids; this.clock = clock;
    }
    public UUID execute(UUID articleId, Integer rank) {
        var commandId = ids.generate();
        authoring.setFeaturedRank(commandId, clock.now(), articleId, rank);
        return commandId;
    }
}
