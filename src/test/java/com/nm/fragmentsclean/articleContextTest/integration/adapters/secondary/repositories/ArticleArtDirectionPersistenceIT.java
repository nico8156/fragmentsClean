package com.nm.fragmentsclean.articleContextTest.integration.adapters.secondary.repositories;

import com.nm.fragmentsclean.articleContextTest.integration.AbstractJpaIntegrationTest;
import com.nm.fragmentsclean.articleContext.write.adapters.secondary.gateways.repositories.JdbcArticleAuthoringSagaRepository;
import com.nm.fragmentsclean.articleContext.write.businesslogic.processManagers.*;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation.ArticleArtDirection;
import com.nm.fragmentsclean.articleContext.read.GetArticleGenerationReviewQueryHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.Instant;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

class ArticleArtDirectionPersistenceIT extends AbstractJpaIntegrationTest {
    @Autowired JdbcTemplate jdbc;

    @Test void persistsDirectionAcrossStateTransitionsAndExposesItInTheStudioSnapshot() {
        var repository = new JdbcArticleAuthoringSagaRepository(jdbc);
        var now = Instant.parse("2026-10-07T10:00:00Z");
        var saga = ArticleAuthoringSaga.request(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "Café et silence", ArticleAuthoringTrigger.MANUAL, now, ArticleArtDirection.CONTEMPLATIVE);
        repository.save(saga);
        saga.enqueueGeneration(now);
        repository.save(saga);
        var restored = repository.byId(saga.snapshot().sagaId()).orElseThrow();
        assertThat(restored.snapshot()).isEqualTo(saga.snapshot());
        var view = new GetArticleGenerationReviewQueryHandler(jdbc, reference -> reference).handle(saga.snapshot().sagaId());
        assertThat(view.artDirection()).isEqualTo("CONTEMPLATIVE");
    }

    @Test void existingInsertContractsDefaultToTheHistoricalDirection() {
        var id = UUID.randomUUID();
        jdbc.update("INSERT INTO article_authoring_sagas (saga_id,article_id,revision_id,theme,trigger,state,version,created_at,updated_at) VALUES (?,?,?,'Legacy','MANUAL','REQUESTED',0,now(),now())", id, UUID.randomUUID(), UUID.randomUUID());
        assertThat(new JdbcArticleAuthoringSagaRepository(jdbc).byId(id).orElseThrow().snapshot().artDirection()).isEqualTo(ArticleArtDirection.ORIGINAL);
    }
}
