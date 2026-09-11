package com.nm.fragmentsclean.ticketContext.read.projections;

import org.springframework.stereotype.Component;
import com.nm.fragmentsclean.ticketContext.read.adapters.secondary.repositories.JdbcTicketStatusProjectionRepository;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketAdminUpdatedEvent;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketAdminDeletedEvent;

@Component
public class TicketAdminEventHandlers {
    private final JdbcTicketStatusProjectionRepository repository;
    public TicketAdminEventHandlers(JdbcTicketStatusProjectionRepository repository) { this.repository = repository; }
    public boolean updated(TicketAdminUpdatedEvent event) { return repository.applyAdminUpdated(event); }
    public boolean deleted(TicketAdminDeletedEvent event) { return repository.applyAdminDeleted(event); }
}
