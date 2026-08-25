package com.nm.fragmentsclean.ticketContext.write.businesslogic.models;

import java.time.Instant;
import java.util.UUID;

public record TicketAdminUpdatedEvent(UUID eventId, UUID commandId, UUID ticketId, UUID userId,
		Ticket.TicketSnapshot snapshot, UUID actorUserId, Instant occurredAt) implements com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent { }
