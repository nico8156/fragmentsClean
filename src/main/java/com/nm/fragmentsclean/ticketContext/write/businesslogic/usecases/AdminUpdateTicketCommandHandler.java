package com.nm.fragmentsclean.ticketContext.write.businesslogic.usecases;

import jakarta.transaction.Transactional;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketRepository;

@Transactional
public class AdminUpdateTicketCommandHandler {
    private final TicketRepository repository; private final DomainEventPublisher publisher; private final DateTimeProvider clock;
    public AdminUpdateTicketCommandHandler(TicketRepository repository, DomainEventPublisher publisher, DateTimeProvider clock) {
        this.repository = repository; this.publisher = publisher; this.clock = clock;
    }
    public void execute(AdminUpdateTicketCommand command) {
        var ticket = repository.byId(command.ticketId()).orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
        ticket.adminUpdate(command.commandId(), command.actorUserId(), command.update(), clock.now()); repository.save(ticket);
        ticket.domainEvents().forEach(publisher::publish); ticket.clearDomainEvents();
    }
}
