package com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories.ArticleAuthoringSagaRepository;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories.ArticleGenerationShellRepository;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.ArticleAuthoringObservability;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleAggregate;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleGenerationRequestedEvent;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.processManagers.ArticleAuthoringSaga;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.CommandStatusRecorder;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Objects;
import java.util.UUID;

@Component
@Transactional
public class RequestArticleGenerationCommandHandler implements CommandHandler<RequestArticleGenerationCommand> {
    private final ArticleAuthoringSagaRepository sagas;
    private final ArticleGenerationShellRepository articles;
    private final DomainEventPublisher events;
    private final DateTimeProvider clock;
    private final CommandStatusRecorder statuses;
    private final ArticleAuthoringObservability observability;

    public RequestArticleGenerationCommandHandler(ArticleAuthoringSagaRepository sagas, ArticleGenerationShellRepository articles, DomainEventPublisher events,
                                                  DateTimeProvider clock, CommandStatusRecorder statuses) {
        this(sagas, articles, events, clock, statuses, ArticleAuthoringObservability.noop());
    }

    @Autowired
    public RequestArticleGenerationCommandHandler(ArticleAuthoringSagaRepository sagas, ArticleGenerationShellRepository articles, DomainEventPublisher events,
                                                  DateTimeProvider clock, CommandStatusRecorder statuses,
                                                  ArticleAuthoringObservability observability) {
        this.sagas = sagas; this.articles = articles; this.events = events; this.clock = clock; this.statuses = statuses;
        this.observability = observability;
    }

    @Override public void execute(RequestArticleGenerationCommand command) {
        Objects.requireNonNull(command, "command");
        if (statuses.isApplied(command.commandId())) return;
        if (sagas.byId(command.sagaId()).isPresent()) throw new IllegalStateException("Generation saga already exists");
        var now = clock.now();
        articles.save(ArticleAggregate.awaitingGeneration(command.articleId(), command.slug(), command.locale(),
                command.authorId(), command.authorName(), now));
        var saga = ArticleAuthoringSaga.request(command.sagaId(), command.articleId(), command.revisionId(),
                command.theme(), command.trigger(), now);
        // Persist the REQUESTED snapshot first. The transition increments the
        // optimistic version and must therefore be persisted as an UPDATE.
        sagas.save(saga);
        saga.enqueueGeneration(now);
        sagas.save(saga);
        var s = saga.snapshot();
        events.publish(new ArticleGenerationRequestedEvent(UUID.randomUUID(), command.commandId(), s.sagaId(),
                s.articleId(), s.revisionId(), s.theme(), command.locale(), s.trigger(), s.version(), now, command.clientAt()));
        statuses.markApplied(command.commandId(), "ArticleAuthoringSaga", s.sagaId().toString(),
                "article.generation.requested", now);
        observability.generationRequested(command.trigger());
    }
}
