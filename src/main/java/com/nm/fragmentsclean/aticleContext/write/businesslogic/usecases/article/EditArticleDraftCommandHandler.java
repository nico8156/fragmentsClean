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
public class EditArticleDraftCommandHandler implements CommandHandler<EditArticleDraftCommand> {
    private final ArticleAggregateRepository repository;
    private final DomainEventPublisher events;
    private final DateTimeProvider clock;
    private final CommandStatusRecorder statuses;

    public EditArticleDraftCommandHandler(ArticleAggregateRepository repository,
                                          DomainEventPublisher events,
                                          DateTimeProvider clock,
                                          CommandStatusRecorder statuses) {
        this.repository = repository;
        this.events = events;
        this.clock = clock;
        this.statuses = statuses;
    }

    @Override
    public void execute(EditArticleDraftCommand command) {
        if (statuses.isApplied(command.commandId())) return;
        var article = repository.byId(command.articleId())
                .orElseThrow(() -> new IllegalStateException("Article draft not found"));
        if (!command.revisionId().equals(article.workingRevisionId())) {
            throw new IllegalArgumentException("Working revision mismatch");
        }
        var now = clock.now();
        article.replaceWorkingDraft(command.draft(), now);
        article.registerDraftEdited(command.commandId(), command.clientAt(), now);
        repository.save(article);
        article.domainEvents().forEach(events::publish);
        article.clearDomainEvents();
        statuses.markApplied(command.commandId(), "Article", article.id().toString(),
                "article.draft.edited", now);
    }
}
