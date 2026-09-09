package com.nm.fragmentsclean.articleContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleAuthoringObservability;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.repositories.ArticleAuthoringSagaRepository;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.repositories.ArticleGenerationShellRepository;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleDomainException;
import com.nm.fragmentsclean.articleContext.write.businesslogic.processManagers.ArticleAuthoringSaga;
import com.nm.fragmentsclean.articleContext.write.businesslogic.processManagers.ArticleAuthoringTrigger;
import com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article.RequestArticleGenerationCommand;
import com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article.RequestArticleGenerationCommandHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.CommandStatusRecorder;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RequestArticleGenerationCommandHandlerTest {
    @Test
    void rejects_an_invalid_theme_before_creating_a_shell_saga_or_outbox_event() {
        var writes = new AtomicInteger();
        ArticleAuthoringSagaRepository sagas = new ArticleAuthoringSagaRepository() {
            @Override public Optional<ArticleAuthoringSaga> byId(UUID ignored) { return Optional.empty(); }
            @Override public void save(ArticleAuthoringSaga ignored) { writes.incrementAndGet(); }
        };
        ArticleGenerationShellRepository articles = ignored -> writes.incrementAndGet();
        var handler = new RequestArticleGenerationCommandHandler(
                sagas,
                articles,
                ignored -> writes.incrementAndGet(),
                () -> Instant.parse("2026-09-09T13:00:00Z"),
                new CommandStatusRecorder() { @Override public void markApplied(UUID id, String type, String aggregateId, String eventType, Instant at) { writes.incrementAndGet(); } },
                ArticleAuthoringObservability.noop());

        assertThatThrownBy(() -> handler.execute(command("x".repeat(241))))
                .isInstanceOf(ArticleDomainException.class)
                .hasMessage("Le sujet de l'article est invalide.");

        assertThat(writes).hasValue(0);
    }

    private static RequestArticleGenerationCommand command(String theme) {
        return new RequestArticleGenerationCommand(
                UUID.randomUUID(), Instant.parse("2026-09-09T12:59:00Z"), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                theme, "article", "fr-FR", UUID.randomUUID(), "Studio", ArticleAuthoringTrigger.MANUAL);
    }
}
