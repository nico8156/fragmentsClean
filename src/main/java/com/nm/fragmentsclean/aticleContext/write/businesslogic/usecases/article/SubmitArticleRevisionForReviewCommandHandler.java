package com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories.ArticleAggregateRepository;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleAggregate;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.CommandStatusRecorder;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Transactional
public class SubmitArticleRevisionForReviewCommandHandler
        implements CommandHandler<SubmitArticleRevisionForReviewCommand> {

    private final ArticleAggregateRepository repository;
    private final DomainEventPublisher eventPublisher;
    private final DateTimeProvider clock;
    private final CommandStatusRecorder commandStatus;

    public SubmitArticleRevisionForReviewCommandHandler(ArticleAggregateRepository repository,
                                                        DomainEventPublisher eventPublisher,
                                                        DateTimeProvider clock,
                                                        CommandStatusRecorder commandStatus) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
        this.commandStatus = commandStatus;
    }

    @Override
    public void execute(SubmitArticleRevisionForReviewCommand command) {
        Objects.requireNonNull(command, "La commande est obligatoire.");
        if (commandStatus.isApplied(command.commandId())) {
            return;
        }
        var article = repository.byId(command.articleId())
                .orElseThrow(() -> new IllegalArgumentException("Article introuvable."));
        var now = clock.now();
        article.submitForReview(now);
        article.registerRevisionSubmitted(command.commandId(), command.clientAt(), now);
        repository.save(article);
        article.domainEvents().forEach(eventPublisher::publish);
        article.clearDomainEvents();
        commandStatus.markApplied(command.commandId(), "Article", article.id().toString(),
                "article.revision.submitted", now);
    }
}
