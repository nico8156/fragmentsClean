package com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories.ArticleRepository;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.Article;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import jakarta.transaction.Transactional;

@Transactional
public class CreateArticleCommandHandler implements CommandHandler<CreateArticleCommand> {

    private final ArticleRepository articleRepository;
    private final DomainEventPublisher eventPublisher;
    private final DateTimeProvider dateTimeProvider;

    public CreateArticleCommandHandler(ArticleRepository articleRepository,
                                       DomainEventPublisher eventPublisher,
                                       DateTimeProvider dateTimeProvider) {
        this.articleRepository = articleRepository;
        this.eventPublisher = eventPublisher;
        this.dateTimeProvider = dateTimeProvider;
    }

    @Override
    public void execute(CreateArticleCommand cmd)  {
        var now = dateTimeProvider.now();

        // idempotence simple : si l’article existe déjà, on ne recrée pas
        var existing = articleRepository.byId(cmd.articleId());
        if (existing.isPresent()) {
            // TODO: comme pour les commentaires, on pourrait vérifier cohérence slug/locale/author
            return;
        }

        var article = Article.createNew(
                cmd.articleId(),
                cmd.slug(),
                cmd.locale(),
                cmd.authorId(),
                cmd.authorName(),
                cmd.title(),
                cmd.intro(),
                cmd.blocksJson(),
                cmd.conclusion(),
                cmd.coverUrl(),
                cmd.coverWidth(),
                cmd.coverHeight(),
                cmd.coverAlt(),
                cmd.tags(),
                cmd.readingTimeMin(),
                cmd.coffeeIds(),
                now
        );

        articleRepository.save(article);

        article.registerCreatedEvent(
                cmd.commandId(),
                cmd.clientAt(),
                now
        );

        article.domainEvents().forEach(eventPublisher::publish);
        article.clearDomainEvents();
    }
}
