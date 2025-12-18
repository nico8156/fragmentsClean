package com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways;

import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.Ticket;

import java.util.Optional;
import java.util.UUID;

public interface TicketRepository {
    Optional<Ticket> byId(UUID ticketId);

    void save(Ticket ticket);
}
