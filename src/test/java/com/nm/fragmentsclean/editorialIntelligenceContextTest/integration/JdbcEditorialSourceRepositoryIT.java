package com.nm.fragmentsclean.editorialIntelligenceContextTest.integration;

import com.nm.fragmentsclean.coffeeContextTest.integration.AbstractReadJdbcIntegrationTest;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.adapters.secondary.gateways.repositories.JdbcEditorialSourceRepository;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.*;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases.ClaimEditorialSourceConsultationCommand;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases.ClaimEditorialSourceConsultationCommandHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcEditorialSourceRepositoryIT extends AbstractReadJdbcIntegrationTest {
    @Autowired JdbcEditorialSourceRepository repository;
    @Autowired JdbcTemplate jdbc;
    @Autowired ClaimEditorialSourceConsultationCommandHandler claimHandler;
    private final UUID sourceId = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private final Instant now = Instant.parse("2026-09-08T10:00:00Z");

    @AfterEach void cleanup() { jdbc.update("DELETE FROM editorial_sources WHERE source_id = ?", sourceId); }

    @Test
    void persists_checkpoint_and_returns_only_due_unleased_sources() {
        var source = EditorialSource.register(sourceId, "Perfect Daily Grind", EditorialSourceAccessMode.RSS,
                EditorialAuthorityLevel.SPECIALIZED_MEDIA, "https://perfectdailygrind.com/feed/", Duration.ofHours(6), now);
        repository.save(source);

        assertThat(repository.dueAt(now, 10)).extracting(item -> item.snapshot().id()).contains(sourceId);

        source.claimConsultation("worker-a", now, now.plusSeconds(60));
        repository.save(source);
        assertThat(repository.dueAt(now.plusSeconds(1), 10)).isEmpty();

        source.completeConsultation(2, "etag-1", "external-1", now.minusSeconds(5), now.plusSeconds(2));
        repository.save(source);

        var restored = repository.byId(sourceId).orElseThrow().snapshot();
        assertThat(restored.checkpoint().lastExternalId()).isEqualTo("external-1");
        assertThat(restored.lastSuccessfulCheckAt()).isEqualTo(now.plusSeconds(2));
        assertThat(restored.nextCheckAt()).isEqualTo(now.plus(Duration.ofHours(6)).plusSeconds(2));
    }

    @Test
    void claim_command_persists_a_lease_and_removes_the_source_from_the_due_set() {
		var runtimeNow = Instant.now();
        var source = EditorialSource.register(sourceId, "SCA", EditorialSourceAccessMode.RSS,
                EditorialAuthorityLevel.AUTHORITATIVE, "https://sca.coffee/news", Duration.ofHours(6), runtimeNow);
        repository.save(source);

        claimHandler.execute(new ClaimEditorialSourceConsultationCommand(sourceId, "integration-worker", runtimeNow.plusSeconds(60)));

        assertThat(repository.byId(sourceId).orElseThrow().snapshot().leaseOwner()).isEqualTo("integration-worker");
    }
}
