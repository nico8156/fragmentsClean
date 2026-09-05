package com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.repositories.ArticleRepository;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.Article;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.CommandStatusRecorder;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import jakarta.transaction.Transactional;

@Transactional
public class CreateArticleCommandHandler implements CommandHandler<CreateArticleCommand> {

    private final ArticleRepository articleRepository;
    private final DomainEventPublisher eventPublisher;
    private final DateTimeProvider dateTimeProvider;
    private final CommandStatusRecorder commandStatusRecorder;

    public CreateArticleCommandHandler(ArticleRepository articleRepository,
                                       DomainEventPublisher eventPublisher,
                                       DateTimeProvider dateTimeProvider,
                                       CommandStatusRecorder commandStatusRecorder) {
        this.articleRepository = articleRepository;
        this.eventPublisher = eventPublisher;
        this.dateTimeProvider = dateTimeProvider;
        this.commandStatusRecorder = commandStatusRecorder;
    }

    @Override
    public void execute(CreateArticleCommand cmd)  {
        var now = dateTimeProvider.now();

        if (commandStatusRecorder.isApplied(cmd.commandId())) {
            return;
        }

        // idempotence simple : si l’article existe déjà, on ne recrée pas
        var existing = articleRepository.byId(cmd.articleId());
        if (existing.isPresent()) {
            commandStatusRecorder.markApplied(
                    cmd.commandId(),
                    "Article",
                    cmd.articleId().toString(),
                    "article.created",
                    now
            );
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

        commandStatusRecorder.markApplied(
                cmd.commandId(),
                "Article",
                cmd.articleId().toString(),
                "article.created",
                now
        );
    }
}
