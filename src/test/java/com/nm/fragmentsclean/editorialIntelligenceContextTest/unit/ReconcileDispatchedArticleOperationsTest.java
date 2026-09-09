package com.nm.fragmentsclean.editorialIntelligenceContextTest.unit;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.EditorialPublicationScheduleRepository;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.ScheduledCommandOutcomePort;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.EditorialPublicationSchedule;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases.ReconcileDispatchedArticleOperations;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class ReconcileDispatchedArticleOperationsTest {
    @Test void completes_only_after_the_article_command_is_applied() {
        var repository = new FakeSchedules();
        var now = Instant.parse("2026-09-08T10:00:00Z");
        var schedule = EditorialPublicationSchedule.schedule(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                EditorialPublicationSchedule.Operation.PUBLISH, now, now);
        schedule.claim("worker", now, now.plusSeconds(30));
        schedule.markDispatched("worker", now.plusSeconds(1));
        repository.save(schedule);

        var useCase = new ReconcileDispatchedArticleOperations(repository,
                id -> new ScheduledCommandOutcomePort.Outcome(ScheduledCommandOutcomePort.Outcome.Status.APPLIED, null));

        assertThat(useCase.execute(10)).isEqualTo(1);
        assertThat(repository.byId(schedule.snapshot().id()).orElseThrow().snapshot().status())
                .isEqualTo(EditorialPublicationSchedule.Status.COMPLETED);
    }

    private static final class FakeSchedules implements EditorialPublicationScheduleRepository {
        private final Map<UUID, EditorialPublicationSchedule> values = new HashMap<>();
        public void save(EditorialPublicationSchedule schedule) { values.put(schedule.snapshot().id(), schedule); }
        public Optional<EditorialPublicationSchedule> byId(UUID id) { return Optional.ofNullable(values.get(id)); }
        public List<EditorialPublicationSchedule> claimableAt(Instant now, int limit) { return List.of(); }
        public List<EditorialPublicationSchedule> dispatched(int limit) { return values.values().stream().filter(v -> v.snapshot().status() == EditorialPublicationSchedule.Status.DISPATCHED).limit(limit).toList(); }
    }
}
