package com.nm.fragmentsclean.ticketContext.read.projections;

import org.springframework.stereotype.Component;
import com.nm.fragmentsclean.ticketContext.read.adapters.secondary.repositories.JdbcTicketStatusProjectionRepository;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketAdminUpdatedEvent;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketAdminDeletedEvent;

@Component
public class TicketAdminEventHandlers {
    private final JdbcTicketStatusProjectionRepository repository;
    public TicketAdminEventHandlers(JdbcTicketStatusProjectionRepository repository) { this.repository = repository; }
    public void updated(TicketAdminUpdatedEvent event) { repository.applyAdminUpdated(event); }
    public void deleted(TicketAdminDeletedEvent event) { repository.applyAdminDeleted(event); }
}
