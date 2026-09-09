package com.nm.fragmentsclean.editorialIntelligenceContextTest.integration;

import com.nm.fragmentsclean.coffeeContextTest.integration.AbstractReadJdbcIntegrationTest;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.adapters.secondary.gateways.repositories.JdbcEditorialPublicationScheduleRepository;
import com.nm.fragmentsclean.editorialIntelligenceContext.read.adapters.secondary.JdbcEditorialCalendarCatalog;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.EditorialPublicationSchedule;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.EditorialScheduleConcurrencyException;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases.ScheduleArticleOperation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdbcEditorialPublicationScheduleRepositoryIT extends AbstractReadJdbcIntegrationTest {
    @Autowired JdbcEditorialPublicationScheduleRepository repository;
    @Autowired JdbcEditorialCalendarCatalog calendar;
    @Autowired ScheduleArticleOperation scheduleArticleOperation;
    @Autowired JdbcTemplate jdbc;
    @AfterEach void cleanup() { jdbc.update("DELETE FROM editorial_publication_schedule"); }

    @Test void round_trips_lease_and_uses_optimistic_versioning() {
        var now = Instant.parse("2026-09-08T10:00:00Z");
        var schedule = EditorialPublicationSchedule.schedule(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                EditorialPublicationSchedule.Operation.PUBLISH, now, now);
        repository.save(schedule);
        var stale = repository.byId(schedule.snapshot().id()).orElseThrow();
        var current = repository.byId(schedule.snapshot().id()).orElseThrow();

        current.claim("worker", now, now.plusSeconds(30));
        repository.save(current);
        assertThat(repository.claimableAt(now.plusSeconds(1), 10)).isEmpty();

        stale.claim("stale", now, now.plusSeconds(20));
        assertThatThrownBy(() -> repository.save(stale)).isInstanceOf(EditorialScheduleConcurrencyException.class);
        assertThat(repository.claimableAt(now.plusSeconds(30), 10)).hasSize(1);
    }

    @Test void schedules_an_article_operation_and_exposes_it_in_the_monthly_calendar() {
        var scheduleId = UUID.randomUUID();
        var articleId = UUID.randomUUID();
        var revisionId = UUID.randomUUID();
        var dueAt = Instant.now().plusSeconds(3_600);

        scheduleArticleOperation.execute(scheduleId, articleId, revisionId, "PUBLISH", dueAt);

        assertThat(calendar.month(java.time.YearMonth.from(dueAt.atZone(java.time.ZoneOffset.UTC))))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.scheduleId()).isEqualTo(scheduleId);
                    assertThat(item.articleId()).isEqualTo(articleId);
                    assertThat(item.revisionId()).isEqualTo(revisionId);
                    assertThat(item.operation()).isEqualTo("PUBLISH");
                    assertThat(item.status()).isEqualTo("SCHEDULED");
                });
    }
}
