package com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article;

import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.repositories.ArticleAggregateRepository;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.repositories.ArticlePublicationCapacityPort;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticlePublicationPolicy;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.CommandStatusRecorder;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Transactional
public class PublishArticleRevisionCommandHandler implements CommandHandler<PublishArticleRevisionCommand> {

    private final ArticleAggregateRepository repository;
    private final DomainEventPublisher eventPublisher;
    private final DateTimeProvider clock;
    private final CommandStatusRecorder commandStatus;
    private final ArticlePublicationCapacityPort publicationCapacity;
    private final ArticlePublicationPolicy publicationPolicy;

    public PublishArticleRevisionCommandHandler(ArticleAggregateRepository repository,
                                                DomainEventPublisher eventPublisher,
                                                DateTimeProvider clock,
                                                CommandStatusRecorder commandStatus) {
        this(repository, eventPublisher, clock, commandStatus, articleId -> 0, new ArticlePublicationPolicy());
    }

    @Autowired
    public PublishArticleRevisionCommandHandler(ArticleAggregateRepository repository,
                                                DomainEventPublisher eventPublisher,
                                                DateTimeProvider clock,
                                                CommandStatusRecorder commandStatus,
                                                ArticlePublicationCapacityPort publicationCapacity) {
        this(repository, eventPublisher, clock, commandStatus, publicationCapacity, new ArticlePublicationPolicy());
    }

    public PublishArticleRevisionCommandHandler(ArticleAggregateRepository repository,
                                                DomainEventPublisher eventPublisher,
                                                DateTimeProvider clock,
                                                CommandStatusRecorder commandStatus,
                                                ArticlePublicationCapacityPort publicationCapacity,
                                                ArticlePublicationPolicy publicationPolicy) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.commandStatus = commandStatus;
        this.publicationCapacity = publicationCapacity;
        this.publicationPolicy = publicationPolicy;
    }

    @Override
    public void execute(PublishArticleRevisionCommand command) {
        Objects.requireNonNull(command, "La commande est obligatoire.");
        if (commandStatus.isApplied(command.commandId())) {
            return;
        }
        var article = repository.byId(command.articleId())
                .orElseThrow(() -> new IllegalArgumentException("Article introuvable."));
        if (!article.workingRevisionId().equals(command.expectedRevisionId())) {
            throw new IllegalStateException("La révision de travail est obsolète.");
        }
        publicationPolicy.evaluate(publicationCapacity.countPublishedExcluding(article.id()));
        var now = clock.now();
        article.publishWorkingRevision(now);
        article.registerRevisionPublished(command.commandId(), command.clientAt(), now);
        repository.save(article);
        article.domainEvents().forEach(eventPublisher::publish);
        article.clearDomainEvents();
        commandStatus.markApplied(command.commandId(), "Article", article.id().toString(),
                "article.revision.published", now);
    }
}
