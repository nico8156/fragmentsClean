package com.nm.fragmentsclean.aticleContext.write.businesslogic.usecases.article;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.repositories.ArticleAuthoringSagaRepository;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleGenerationRequestedEvent;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.processManagers.ArticleAuthoringSaga;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.CommandStatusRecorder;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;
import java.util.Objects;
import java.util.UUID;

@Component
@Transactional
public final class RequestArticleGenerationCommandHandler implements CommandHandler<RequestArticleGenerationCommand> {
    private final ArticleAuthoringSagaRepository sagas;
    private final DomainEventPublisher events;
    private final DateTimeProvider clock;
    private final CommandStatusRecorder statuses;

    public RequestArticleGenerationCommandHandler(ArticleAuthoringSagaRepository sagas, DomainEventPublisher events,
                                                  DateTimeProvider clock, CommandStatusRecorder statuses) {
        this.sagas = sagas; this.events = events; this.clock = clock; this.statuses = statuses;
    }

    @Override public void execute(RequestArticleGenerationCommand command) {
        Objects.requireNonNull(command, "command");
        if (statuses.isApplied(command.commandId())) return;
        if (sagas.byId(command.sagaId()).isPresent()) throw new IllegalStateException("Generation saga already exists");
        var now = clock.now();
        var saga = ArticleAuthoringSaga.request(command.sagaId(), command.articleId(), command.revisionId(),
                command.theme(), command.trigger(), now);
        saga.enqueueGeneration(now);
        sagas.save(saga);
        var s = saga.snapshot();
        events.publish(new ArticleGenerationRequestedEvent(UUID.randomUUID(), command.commandId(), s.sagaId(),
                s.articleId(), s.revisionId(), s.theme(), command.locale(), s.trigger(), s.version(), now, command.clientAt()));
        statuses.markApplied(command.commandId(), "ArticleAuthoringSaga", s.sagaId().toString(),
                "article.generation.requested", now);
    }
}
