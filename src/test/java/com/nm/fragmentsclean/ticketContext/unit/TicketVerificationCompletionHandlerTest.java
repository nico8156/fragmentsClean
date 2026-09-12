package com.nm.fragmentsclean.ticketContext.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventPublisher.FakeDomainEventPublisher;
import com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.fake.FakeTicketRepository;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketVerificationProvider;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.Ticket;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerificationCompletedEvent;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.processManagers.TicketVerificationCompletionHandler;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.processManagers.TicketVerificationJob;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.processManagers.TicketVerificationLeaseClaimer;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TicketVerificationCompletionHandlerTest {
    private static final Instant NOW = Instant.parse("2026-09-12T08:00:00Z");
    private final UUID ticketId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private TicketVerificationProcessManagerTest.FakeJobs jobs;
    private FakeTicketRepository tickets;
    private FakeDomainEventPublisher events;
    private TicketVerificationCompletionHandler handler;

    @BeforeEach void setUp() {
        jobs = new TicketVerificationProcessManagerTest.FakeJobs();
        tickets = new FakeTicketRepository();
        events = new FakeDomainEventPublisher();
        tickets.save(Ticket.createNewAnalyzing(ticketId, userId, "TOTAL", null, NOW));
        handler = new TicketVerificationCompletionHandler(jobs, tickets, events);
    }

    @Test void approved_completion_updates_ticket_job_and_outbox_event() {
        var work = runningWork();
        handler.complete(work, new TicketVerificationProvider.Approved(400, "EUR", null, "CAFE", null, null, List.of(), "trace"), NOW.plusSeconds(1));

        assertThat(tickets.byId(ticketId).orElseThrow().toSnapshot().status()).isEqualTo(Ticket.TicketStatus.CONFIRMED);
        assertThat(jobs.byId(work.job().jobId()).orElseThrow().snapshot().state()).isEqualTo(TicketVerificationJob.State.COMPLETED);
        assertThat(events.published).singleElement().isInstanceOf(TicketVerificationCompletedEvent.class);
    }

    @Test void final_technical_failure_is_not_a_business_rejection_and_can_be_retried_later() {
        var work = runningWork();
        handler.failFinal(work, "provider unavailable", "trace", NOW.plusSeconds(1));

        var ticket = tickets.byId(ticketId).orElseThrow();
        assertThat(ticket.toSnapshot().status()).isEqualTo(Ticket.TicketStatus.FAILED);
        assertThat(ticket.markAnalyzingIfPossible("TOTAL", null, NOW.plusSeconds(2))).isTrue();
        assertThat(events.published).singleElement().satisfies(event ->
                assertThat(((TicketVerificationCompletedEvent) event).outcome()).isEqualTo(TicketVerificationCompletedEvent.Outcome.FAILED_FINAL));
    }

    @Test void stale_worker_completion_is_ignored() {
        var first = runningWork();
        var job = jobs.byId(first.job().jobId()).orElseThrow();
        job.retry("worker", "timeout", NOW.plusSeconds(1), NOW.plusSeconds(2)); jobs.save(job);
        job = jobs.byId(first.job().jobId()).orElseThrow(); job.claim("worker-2", NOW.plusSeconds(2), Duration.ofSeconds(30)); jobs.save(job);

        assertThat(handler.complete(first, new TicketVerificationProvider.Rejected("NO", "no", "trace"), NOW.plusSeconds(3))).isFalse();
        assertThat(events.published).isEmpty();
    }

    @Test void exhausted_retry_does_not_override_a_ticket_already_completed_elsewhere() {
        var work = runningWork();
        var ticket = tickets.byId(ticketId).orElseThrow();
        ticket.confirm(new Ticket.ConfirmResult(400, "EUR", null, "CAFE", null, null, List.of()), NOW.plusSeconds(1));
        tickets.save(ticket);

        assertThat(handler.failFinal(work, "provider unavailable", "trace", NOW.plusSeconds(2))).isFalse();

        assertThat(tickets.byId(ticketId).orElseThrow().toSnapshot().status()).isEqualTo(Ticket.TicketStatus.CONFIRMED);
        assertThat(jobs.byId(work.job().jobId()).orElseThrow().snapshot().state()).isEqualTo(TicketVerificationJob.State.COMPLETED);
        assertThat(events.published).isEmpty();
    }

    private TicketVerificationLeaseClaimer.Work runningWork() {
        var job = TicketVerificationJob.request(UUID.randomUUID(), UUID.randomUUID(), ticketId, userId, "TOTAL", null, NOW, NOW);
        jobs.save(job); job = jobs.byId(job.snapshot().jobId()).orElseThrow(); job.claim("worker", NOW, Duration.ofSeconds(30)); jobs.save(job);
        return new TicketVerificationLeaseClaimer.Work(job.snapshot());
    }
}
