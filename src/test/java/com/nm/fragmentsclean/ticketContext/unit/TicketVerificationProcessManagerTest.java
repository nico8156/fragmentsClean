package com.nm.fragmentsclean.ticketContext.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import com.nm.fragmentsclean.ticketContext.write.adapters.secondary.gateways.fake.FakeTicketRepository;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketVerificationJobRepository;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.Ticket;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerifyAcceptedEvent;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.processManagers.TicketVerificationJob;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.processManagers.TicketVerificationProcessManager;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TicketVerificationProcessManagerTest {
    private static final UUID EVENT_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID COMMAND_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID TICKET_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void accepted_event_persists_one_pending_job_and_does_not_call_the_provider() {
        var tickets = new FakeTicketRepository();
        var clock = new DeterministicDateTimeProvider();
        tickets.save(Ticket.createNewAnalyzing(TICKET_ID, USER_ID, "TOTAL 4,00", "s3://ticket", clock.now()));
        var jobs = new FakeJobs();
        var manager = new TicketVerificationProcessManager(tickets, jobs, clock);

        manager.handle(event());
        manager.handle(event());

        assertThat(jobs.values).hasSize(1);
        var job = jobs.byId(EVENT_ID).orElseThrow().snapshot();
        assertThat(job.state()).isEqualTo(TicketVerificationJob.State.PENDING);
        assertThat(job.ticketId()).isEqualTo(TICKET_ID);
        assertThat(job.commandId()).isEqualTo(COMMAND_ID);
    }

    private TicketVerifyAcceptedEvent event() {
        return new TicketVerifyAcceptedEvent(EVENT_ID, COMMAND_ID, TICKET_ID, USER_ID, "TOTAL 4,00", "s3://ticket",
                Ticket.TicketStatus.ANALYZING.name(), 0, Instant.parse("2026-09-12T08:00:00Z"),
                Instant.parse("2026-09-12T07:59:00Z"));
    }

    static final class FakeJobs implements TicketVerificationJobRepository {
        final LinkedHashMap<UUID, TicketVerificationJob> values = new LinkedHashMap<>();
        @Override public Optional<TicketVerificationJob> byId(UUID id) { return Optional.ofNullable(values.get(id)); }
        @Override public void save(TicketVerificationJob job) { values.put(job.snapshot().jobId(), TicketVerificationJob.reconstitute(job.snapshot())); }
        @Override public List<UUID> claimableIds(Instant now, int limit) { return values.values().stream().filter(job -> job.claimableAt(now)).limit(limit).map(job -> job.snapshot().jobId()).toList(); }
    }
}
