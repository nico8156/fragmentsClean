package com.nm.fragmentsclean.ticketContext.write.businesslogic.usecases;

import java.time.Instant;
import java.util.UUID;
import java.util.List;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.Ticket;

public record AdminUpdateTicketCommand(UUID commandId, UUID ticketId, UUID actorUserId,
        Ticket.AdminUpdate update, Instant occurredAt) { }
