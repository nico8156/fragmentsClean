package com.nm.fragmentsclean.editorialIntelligenceContextTest.unit;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.EditorialPublicationScheduleRepository;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.ScheduledArticleOperationPort;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.ScheduledCommandOutcomePort;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models.EditorialPublicationSchedule;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.usecases.DispatchDueArticleOperations;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

class DispatchDueArticleOperationsTest {
    @Test void claims_then_dispatches_with_the_schedule_as_idempotency_key() {
        Instant now = Instant.parse("2026-09-08T10:00:00Z");
        var repository = new FakeSchedules();
        var schedule = EditorialPublicationSchedule.schedule(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                EditorialPublicationSchedule.Operation.PUBLISH, now, now);
        repository.save(schedule);
        var operations = new FakeOperations();
        var useCase = new DispatchDueArticleOperations(repository, operations, pendingOutcomes(), () -> now,
                new TransactionTemplate(new NoOpTransactionManager()));

        assertThat(useCase.execute(10, Duration.ofMinutes(5), "worker")).isEqualTo(1);
        assertThat(operations.commandId).isEqualTo(schedule.snapshot().id());
        assertThat(repository.byId(schedule.snapshot().id()).orElseThrow().snapshot().status())
                .isEqualTo(EditorialPublicationSchedule.Status.DISPATCHED);
    }

    @Test void leaves_the_claim_recoverable_when_dispatch_fails_before_command_acceptance() {
        Instant now = Instant.parse("2026-09-08T10:00:00Z");
        var repository = new FakeSchedules();
        var schedule = EditorialPublicationSchedule.schedule(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                EditorialPublicationSchedule.Operation.PUBLISH, now, now);
        repository.save(schedule);
        ScheduledArticleOperationPort failing = new ScheduledArticleOperationPort() {
            public void publish(UUID commandId, Instant at, UUID article, UUID revision) { throw new IllegalStateException("transport unavailable"); }
            public void archive(UUID commandId, Instant at, UUID article) { throw new IllegalStateException("transport unavailable"); }
        };
        var useCase = new DispatchDueArticleOperations(repository, failing, pendingOutcomes(), () -> now,
                new TransactionTemplate(new NoOpTransactionManager()));

        assertThat(useCase.execute(10, Duration.ofMinutes(5), "worker")).isZero();
        assertThat(repository.byId(schedule.snapshot().id()).orElseThrow().snapshot().status())
                .isEqualTo(EditorialPublicationSchedule.Status.CLAIMED);
    }

    @Test void records_dispatch_when_the_command_store_proves_acceptance_despite_an_adapter_exception() {
        Instant now = Instant.parse("2026-09-08T10:00:00Z");
        var repository = new FakeSchedules();
        var schedule = EditorialPublicationSchedule.schedule(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                EditorialPublicationSchedule.Operation.PUBLISH, now, now);
        repository.save(schedule);
        ScheduledArticleOperationPort acceptedThenFailing = new ScheduledArticleOperationPort() {
            public void publish(UUID commandId, Instant at, UUID article, UUID revision) { throw new IllegalStateException("late response failure"); }
            public void archive(UUID commandId, Instant at, UUID article) { throw new IllegalStateException("late response failure"); }
        };
        ScheduledCommandOutcomePort rejected = ignored -> new ScheduledCommandOutcomePort.Outcome(
                ScheduledCommandOutcomePort.Outcome.Status.REJECTED, "revision is no longer approved");
        var useCase = new DispatchDueArticleOperations(repository, acceptedThenFailing, rejected, () -> now,
                new TransactionTemplate(new NoOpTransactionManager()));

        assertThat(useCase.execute(10, Duration.ofMinutes(5), "worker")).isEqualTo(1);
        assertThat(repository.byId(schedule.snapshot().id()).orElseThrow().snapshot().status())
                .isEqualTo(EditorialPublicationSchedule.Status.DISPATCHED);
    }

    private static ScheduledCommandOutcomePort pendingOutcomes() {
        return ignored -> new ScheduledCommandOutcomePort.Outcome(
                ScheduledCommandOutcomePort.Outcome.Status.PENDING, null);
    }

    private static final class FakeOperations implements ScheduledArticleOperationPort {
        private UUID commandId;
        public void publish(UUID commandId, Instant at, UUID article, UUID revision) { this.commandId = commandId; }
        public void archive(UUID commandId, Instant at, UUID article) { this.commandId = commandId; }
    }

    private static final class FakeSchedules implements EditorialPublicationScheduleRepository {
        private final Map<UUID, EditorialPublicationSchedule> values = new HashMap<>();
        public void save(EditorialPublicationSchedule value) { values.put(value.snapshot().id(), value); }
        public Optional<EditorialPublicationSchedule> byId(UUID id) { return Optional.ofNullable(values.get(id)); }
        public List<EditorialPublicationSchedule> claimableAt(Instant now, int limit) { return values.values().stream().filter(v -> v.isDueAt(now)).limit(limit).toList(); }
        public List<EditorialPublicationSchedule> dispatched(int limit) { return List.of(); }
    }

    private static final class NoOpTransactionManager extends AbstractPlatformTransactionManager {
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) { }
        @Override protected void doCommit(DefaultTransactionStatus status) { }
        @Override protected void doRollback(DefaultTransactionStatus status) { }
    }
}
