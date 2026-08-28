package com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories.ArticleAggregateRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.CommandStatusRecorder;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

@Component
@Transactional
public final class ArchiveArticleCommandHandler implements CommandHandler<ArchiveArticleCommand> {
    private final ArticleAggregateRepository repository;
    private final DomainEventPublisher events;
    private final DateTimeProvider clock;
    private final CommandStatusRecorder statuses;

    public ArchiveArticleCommandHandler(ArticleAggregateRepository repository, DomainEventPublisher events,
                                        DateTimeProvider clock, CommandStatusRecorder statuses) {
        this.repository = repository; this.events = events; this.clock = clock; this.statuses = statuses;
    }

    @Override public void execute(ArchiveArticleCommand command) {
        if (statuses.isApplied(command.commandId())) return;
        var article = repository.byId(command.articleId()).orElseThrow();
        var now = clock.now();
        article.archive(now);
        article.registerArchived(command.commandId(), command.clientAt(), now);
        repository.save(article);
        article.domainEvents().forEach(events::publish);
        article.clearDomainEvents();
        statuses.markApplied(command.commandId(), "Article", article.id().toString(), "article.archived", now);
    }
}
