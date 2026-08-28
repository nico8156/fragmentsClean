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

import java.util.Objects;

@Transactional
@Component
public final class CreateArticleDraftCommandHandler implements CommandHandler<CreateArticleDraftCommand> {

    private final ArticleAggregateRepository repository;
    private final DomainEventPublisher eventPublisher;
    private final DateTimeProvider clock;
    private final CommandStatusRecorder commandStatus;

    public CreateArticleDraftCommandHandler(ArticleAggregateRepository repository,
                                            DomainEventPublisher eventPublisher,
                                            DateTimeProvider clock,
                                            CommandStatusRecorder commandStatus) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.commandStatus = commandStatus;
    }

    @Override
    public void execute(CreateArticleDraftCommand command) {
        Objects.requireNonNull(command, "La commande est obligatoire.");
        if (commandStatus.isApplied(command.commandId())) {
            return;
        }
        if (repository.byId(command.articleId()).isPresent()) {
            throw new IllegalStateException("Un article existe déjà avec cet identifiant.");
        }

        var now = clock.now();
        var revision = ArticleRevision.draft(command.revisionId(), command.draft(), now);
        var article = ArticleAggregate.draft(
                command.articleId(), command.slug(), command.locale(), command.authorId(),
                command.authorName(), revision, now);
        repository.save(article);
        article.registerDraftCreated(command.commandId(), command.clientAt(), now);
        publishAndMark(article, command.commandId(), now, "article.draft.created");
    }

    private void publishAndMark(ArticleAggregate article, java.util.UUID commandId,
                                java.time.Instant now, String eventType) {
        article.domainEvents().forEach(eventPublisher::publish);
        article.clearDomainEvents();
        commandStatus.markApplied(commandId, "Article", article.id().toString(), eventType, now);
    }
}
