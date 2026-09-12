package com.nm.fragmentsclean.ticketContext.write.adapters.primary.springboot.scheduling;

import static org.assertj.core.api.Assertions.assertThat;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventPublisher.FakeDomainEventPublisher;
import com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.fake.FakeTicketRepository;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketVerificationJobRepository;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketVerificationProvider;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.Ticket;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.processManagers.TicketVerificationCompletionHandler;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.processManagers.TicketVerificationJob;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.processManagers.TicketVerificationLeaseClaimer;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.processManagers.TicketVerificationProcessManager;
import jakarta.transaction.Transactional;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class ScheduledTicketVerificationWorkerTest {
    @Test
    void provider_runs_outside_transaction_and_completion_is_idempotent() throws Exception {
        var clock = new DeterministicDateTimeProvider();
        var tickets = new FakeTicketRepository();
        var jobs = new FakeJobs();
        var events = new FakeDomainEventPublisher();
        UUID ticketId = UUID.randomUUID(), userId = UUID.randomUUID(), jobId = UUID.randomUUID();
        tickets.save(Ticket.createNewAnalyzing(ticketId, userId, "TOTAL", null, clock.now()));
        jobs.save(TicketVerificationJob.request(jobId, UUID.randomUUID(), ticketId, userId, "TOTAL", null,
                clock.now(), clock.now()));
        TicketVerificationProvider provider = (ocr, image) -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return new TicketVerificationProvider.Approved(400, "EUR", null, "CAFE", null, null, List.of(), "trace");
        };
        var worker = new ScheduledTicketVerificationWorker(jobs, new TicketVerificationLeaseClaimer(jobs), provider,
                new TicketVerificationCompletionHandler(jobs, tickets, events), clock, 30, 5, 5, 10);

        worker.runDue();
        worker.runDue();

        assertThat(tickets.byId(ticketId).orElseThrow().toSnapshot().status()).isEqualTo(Ticket.TicketStatus.CONFIRMED);
        assertThat(jobs.byId(jobId).orElseThrow().snapshot().state()).isEqualTo(TicketVerificationJob.State.COMPLETED);
        assertThat(events.published).hasSize(1);

        Method workerMethod = ScheduledTicketVerificationWorker.class.getDeclaredMethod("process", UUID.class);
        Method intake = TicketVerificationProcessManager.class.getDeclaredMethod("handle",
                com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerifyAcceptedEvent.class);
        Method completion = TicketVerificationCompletionHandler.class.getDeclaredMethod("complete",
                TicketVerificationLeaseClaimer.Work.class, TicketVerificationProvider.Result.class, Instant.class);
        assertThat(workerMethod.isAnnotationPresent(Transactional.class)).isFalse();
        assertThat(intake.isAnnotationPresent(Transactional.class)).isTrue();
        assertThat(completion.isAnnotationPresent(Transactional.class)).isTrue();
    }

    private static final class FakeJobs implements TicketVerificationJobRepository {
        private final LinkedHashMap<UUID, TicketVerificationJob> values = new LinkedHashMap<>();
        @Override public Optional<TicketVerificationJob> byId(UUID id) { return Optional.ofNullable(values.get(id)); }
        @Override public void save(TicketVerificationJob job) { values.put(job.snapshot().jobId(), TicketVerificationJob.reconstitute(job.snapshot())); }
        @Override public List<UUID> claimableIds(Instant now, int limit) { return values.values().stream().filter(job -> job.claimableAt(now)).limit(limit).map(job -> job.snapshot().jobId()).toList(); }
    }
}
