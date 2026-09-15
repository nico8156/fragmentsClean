package com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article;

import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.repositories.ArticleAggregateRepository;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.repositories.ArticleFeaturedRankAvailabilityPort;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleDomainException;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.CommandStatusRecorder;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

@Component
@Transactional
public final class SetArticleFeaturedRankCommandHandler implements CommandHandler<SetArticleFeaturedRankCommand> {
    private final ArticleAggregateRepository articles;
    private final ArticleFeaturedRankAvailabilityPort ranks;
    private final DomainEventPublisher events;
    private final DateTimeProvider clock;
    private final CommandStatusRecorder statuses;
    public SetArticleFeaturedRankCommandHandler(ArticleAggregateRepository articles,
                                               ArticleFeaturedRankAvailabilityPort ranks,
                                               DomainEventPublisher events, DateTimeProvider clock,
                                               CommandStatusRecorder statuses) {
        this.articles = articles; this.ranks = ranks; this.events = events;
        this.clock = clock; this.statuses = statuses;
    }
    @Override public void execute(SetArticleFeaturedRankCommand command) {
        if (statuses.isApplied(command.commandId())) return;
        var article = articles.byId(command.articleId()).orElseThrow(() ->
                new IllegalArgumentException("Article introuvable."));
        var rank = command.featuredRank();
        if (rank != null && ranks.occupiedByAnother(article.id(), rank)) {
            throw new ArticleDomainException("Ce rang à la une est déjà attribué.");
        }
        var now = clock.now();
        if (article.setFeaturedRank(rank, now)) {
            article.registerFeaturedRankChanged(command.commandId(), command.clientAt(), now);
            articles.save(article);
            article.domainEvents().forEach(events::publish);
            article.clearDomainEvents();
        }
        statuses.markApplied(command.commandId(), "Article", article.id().toString(),
                "article.featured_rank.changed", now);
    }
}
