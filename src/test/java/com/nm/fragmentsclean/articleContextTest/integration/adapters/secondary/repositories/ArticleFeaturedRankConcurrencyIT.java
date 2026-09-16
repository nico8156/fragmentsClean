package com.nm.fragmentsclean.articleContextTest.integration.adapters.secondary.repositories;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.articleContextTest.integration.AbstractJpaIntegrationTest;
import com.nm.fragmentsclean.articleContext.write.adapters.secondary.gateways.repositories.JdbcArticleAggregateRepository;
import com.nm.fragmentsclean.articleContext.write.adapters.secondary.gateways.repositories.JdbcArticleFeaturedRankAvailabilityAdapter;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.*;
import com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article.*;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jdbc.CommandStatusRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@Import(CommandStatusRepository.class)
@TestPropertySource(properties = {
        "spring.datasource.hikari.maximum-pool-size=2",
        "spring.datasource.hikari.minimum-idle=0"
})
class ArticleFeaturedRankConcurrencyIT extends AbstractJpaIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @Autowired CommandStatusRepository receipts;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrent_rank_changes_on_one_article_keep_distinct_event_versions() throws Exception {
        var now = Instant.parse("2026-09-16T09:00:00Z");
        var repository = new JdbcArticleAggregateRepository(jdbc, new ObjectMapper());
        var transaction = new TransactionTemplate(transactions);
        var id = UUID.randomUUID();
        var article = ArticleAggregate.draft(id, "same-article-" + id, "fr-FR", UUID.randomUUID(), "Studio",
                ArticleRevision.draft(UUID.randomUUID(), JdbcArticleAggregateRepositoryIT.publishableDraft(), now), now);
        article.submitForReview(now); article.publishWorkingRevision(now);
        transaction.executeWithoutResult(status -> repository.save(article));
        var events = new CopyOnWriteArrayList<com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent>();
        var handler = new SetArticleFeaturedRankCommandHandler(repository, new JdbcArticleFeaturedRankAvailabilityAdapter(jdbc), events::add, () -> now, receipts, receipts);
        var barrier = new CyclicBarrier(2);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var attempts = List.of(1, 2).stream().map(rank -> executor.submit(() -> {
                barrier.await(10, TimeUnit.SECONDS);
                transaction.executeWithoutResult(status -> handler.execute(new SetArticleFeaturedRankCommand(UUID.randomUUID(), now, id, rank)));
                return true;
            })).toList();
            for (var attempt : attempts) assertThat(attempt.get(20, TimeUnit.SECONDS)).isTrue();
            assertThat(events.stream().map(event -> ((ArticleFeaturedRankChangedEvent) event).version()).sorted())
                    .containsExactly(article.version() + 1, article.version() + 2);
            // Reload after both commits, with the same transaction boundary as application use cases.
            // Nested aggregate queries must share one connection even with the release pool of two.
            Long persistedVersion = transaction.execute(status -> repository.byId(id).orElseThrow().version());
            assertThat(persistedVersion).isEqualTo(article.version() + 2);
        } finally {
            executor.shutdownNow();
            transaction.executeWithoutResult(status -> jdbc.update("UPDATE articles SET featured_rank = NULL WHERE article_id = ?", id));
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void concurrent_claims_have_one_winner_one_durable_rejection_and_no_duplicate_rank() throws Exception {
        var now = Instant.parse("2026-09-16T09:00:00Z");
        var repository = new JdbcArticleAggregateRepository(jdbc, new ObjectMapper());
        var ranks = new JdbcArticleFeaturedRankAvailabilityAdapter(jdbc);
        var transaction = new TransactionTemplate(transactions);
        var ids = List.of(UUID.randomUUID(), UUID.randomUUID());
        transaction.executeWithoutResult(status -> {
            for (var id : ids) {
                var article = ArticleAggregate.draft(id, "concurrent-" + id, "fr-FR", UUID.randomUUID(), "Studio",
                        ArticleRevision.draft(UUID.randomUUID(), JdbcArticleAggregateRepositoryIT.publishableDraft(), now), now);
                article.submitForReview(now); article.publishWorkingRevision(now); repository.save(article);
            }
        });
        var barrier = new CyclicBarrier(2);
        var events = new CopyOnWriteArrayList<com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent>();
        var handler = new SetArticleFeaturedRankCommandHandler(repository, (id, rank) -> {
            try { barrier.await(10, TimeUnit.SECONDS); } catch (Exception e) { throw new IllegalStateException(e); }
            return ranks.occupiedByAnother(id, rank);
        }, events::add, () -> now.plusSeconds(1), receipts, receipts);
        var commands = ids.stream().map(id -> new SetArticleFeaturedRankCommand(UUID.randomUUID(), now, id, 5)).toList();
        var executor = Executors.newFixedThreadPool(2);
        try {
            var attempts = commands.stream().map(command -> executor.submit(() -> {
                try { transaction.executeWithoutResult(status -> handler.execute(command)); return "APPLIED"; }
                catch (ArticleDomainException rejected) { return "REJECTED"; }
            })).toList();
            assertThat(List.of(attempts.get(0).get(20, TimeUnit.SECONDS), attempts.get(1).get(20, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("APPLIED", "REJECTED");
            assertThat(jdbc.queryForObject("SELECT count(*) FROM articles WHERE featured_rank = 5 AND status = 'PUBLISHED'", Integer.class)).isEqualTo(1);
            assertThat(events).hasSize(1);
            assertThat(commands.stream().map(command -> receipts.find(command.commandId()).status())).containsExactlyInAnyOrder("APPLIED", "REJECTED");
            var loser = commands.stream().filter(command -> "REJECTED".equals(receipts.find(command.commandId()).status())).findFirst().orElseThrow();
            assertThat(receipts.find(loser.commandId()).reason()).contains("déjà attribué");
            assertThatThrownBy(() -> transaction.executeWithoutResult(status -> handler.execute(loser))).isInstanceOf(ArticleDomainException.class);
            assertThat(receipts.findForRequester(loser.commandId(), UUID.randomUUID()).status()).isEqualTo("PENDING");
        } finally {
            executor.shutdownNow();
            // Only records created by this test; releases the occupied rank for the rest of the suite.
            transaction.executeWithoutResult(status -> ids.forEach(id -> jdbc.update("UPDATE articles SET featured_rank = NULL WHERE article_id = ?", id)));
        }
    }
}
