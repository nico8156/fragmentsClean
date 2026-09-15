package com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article;

import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.repositories.ArticleAggregateRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.CommandStatusRecorder;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

@Component
@Transactional
public final class WithdrawArticleCommandHandler implements CommandHandler<WithdrawArticleCommand> {
    private final ArticleAggregateRepository articles;
    private final DomainEventPublisher events;
    private final DateTimeProvider clock;
    private final CommandStatusRecorder statuses;

    public WithdrawArticleCommandHandler(ArticleAggregateRepository articles, DomainEventPublisher events,
                                         DateTimeProvider clock, CommandStatusRecorder statuses) {
        this.articles = articles; this.events = events; this.clock = clock; this.statuses = statuses;
    }

    @Override public void execute(WithdrawArticleCommand command) {
        if (statuses.isApplied(command.commandId())) return;
        var article = articles.byId(command.articleId()).orElseThrow(() ->
                new IllegalArgumentException("Article introuvable."));
        var now = clock.now();
        article.withdrawToDraft(command.draftRevisionId(), now);
        article.registerWithdrawn(command.commandId(), command.clientAt(), now);
        articles.save(article);
        article.domainEvents().forEach(events::publish);
        article.clearDomainEvents();
        statuses.markApplied(command.commandId(), "Article", article.id().toString(), "article.withdrawn", now);
    }
}
