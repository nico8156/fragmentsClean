package com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.article;

import com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article.ArchiveArticleCommand;
import com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article.PublishArticleRevisionCommand;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.ScheduledArticleOperationPort;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.CommandBus;

import java.time.Instant;
import java.util.UUID;

/** Explicit admin ACL: primitive editorial intent -> article-owned command. */
public final class CommandBusScheduledArticleOperationAdapter implements ScheduledArticleOperationPort {
    private final CommandBus commandBus;
    public CommandBusScheduledArticleOperationAdapter(CommandBus commandBus) { this.commandBus = commandBus; }
    @Override public void publish(UUID commandId, Instant requestedAt, UUID articleId, UUID revisionId) {
        commandBus.dispatch(new PublishArticleRevisionCommand(commandId, requestedAt, articleId, revisionId));
    }
    @Override public void archive(UUID commandId, Instant requestedAt, UUID articleId) {
        commandBus.dispatch(new ArchiveArticleCommand(commandId, requestedAt, articleId));
    }
}
