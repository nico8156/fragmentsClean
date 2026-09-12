package com.nm.fragmentsclean.ticketContext.write.businesslogic.processManagers;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketRepository;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketVerificationJobRepository;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.Ticket;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketVerifyAcceptedEvent;
import jakarta.transaction.Transactional;
import java.util.Objects;

/** Persists the intent; the scheduled worker owns external execution and durable retries. */
public class TicketVerificationProcessManager implements EventHandler<TicketVerifyAcceptedEvent> {
    private final TicketRepository tickets;
    private final TicketVerificationJobRepository jobs;
    private final DateTimeProvider clock;

    public TicketVerificationProcessManager(TicketRepository tickets, TicketVerificationJobRepository jobs,
            DateTimeProvider clock) {
        this.tickets = tickets;
        this.jobs = jobs;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void handle(TicketVerifyAcceptedEvent event) {
        if (jobs.byId(event.eventId()).isPresent()) return;
        var ticket = tickets.byId(event.ticketId())
                .orElseThrow(() -> new IllegalStateException("Ticket not found: " + event.ticketId()));
        var snapshot = ticket.toSnapshot();
        if (!Objects.equals(snapshot.userId(), event.userId())) throw new IllegalStateException("Ticket userId mismatch");
        if (snapshot.status() == Ticket.TicketStatus.CONFIRMED || snapshot.status() == Ticket.TicketStatus.REJECTED
                || snapshot.status() == Ticket.TicketStatus.DELETED) return;
        jobs.save(TicketVerificationJob.request(event.eventId(), event.commandId(), event.ticketId(), event.userId(),
                event.ocrText(), event.imageRef(), event.clientAt(), clock.now()));
    }
}
