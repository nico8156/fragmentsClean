package com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories.ArticleAggregateRepository;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleAggregate;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleRevision;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.CommandStatusRecorder;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

@Component
@Transactional
public class UpsertArticleDraftCommandHandler implements CommandHandler<UpsertArticleDraftCommand> {
    private final ArticleAggregateRepository repository;
    private final DomainEventPublisher events;
    private final DateTimeProvider clock;
    private final CommandStatusRecorder statuses;

    public UpsertArticleDraftCommandHandler(ArticleAggregateRepository repository,
                                            DomainEventPublisher events,
                                            DateTimeProvider clock,
                                            CommandStatusRecorder statuses) {
        this.repository = repository;
        this.events = events;
        this.clock = clock;
        this.statuses = statuses;
    }

    @Override
    public void execute(UpsertArticleDraftCommand command) {
        if (statuses.isApplied(command.commandId())) return;
        var now = clock.now();
        var existing = repository.byId(command.articleId());
        ArticleAggregate article;
        String eventType;
        if (existing.isEmpty()) {
            article = ArticleAggregate.draft(command.articleId(), command.slug(), command.locale(),
                    command.authorId(), command.authorName(),
                    ArticleRevision.draft(command.revisionId(), command.draft(), now), now);
            article.registerDraftCreated(command.commandId(), command.clientAt(), now);
            eventType = "article.draft.created";
        } else {
            article = existing.get();
            assertIdentity(command, article);
            article.replaceWorkingDraft(command.draft(), now);
            article.registerDraftEdited(command.commandId(), command.clientAt(), now);
            eventType = "article.draft.edited";
        }
        repository.save(article);
        article.domainEvents().forEach(events::publish);
        article.clearDomainEvents();
        statuses.markApplied(command.commandId(), "Article", article.id().toString(), eventType, now);
    }

    private void assertIdentity(UpsertArticleDraftCommand command, ArticleAggregate article) {
        if (!command.revisionId().equals(article.workingRevisionId())) {
            throw new IllegalArgumentException("Working revision mismatch");
        }
        if (!command.slug().equals(article.slug()) || !command.locale().equals(article.locale())
                || !command.authorId().equals(article.authorId())) {
            throw new IllegalArgumentException("Article identity is immutable");
        }
    }
}
