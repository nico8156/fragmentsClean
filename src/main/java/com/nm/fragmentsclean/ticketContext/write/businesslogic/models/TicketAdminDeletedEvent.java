package com.nm.fragmentsclean.ticketContext.write.businesslogic.models;

import java.time.Instant;
import java.util.UUID;

public record TicketAdminDeletedEvent(UUID eventId, UUID commandId, UUID ticketId, UUID userId,
		UUID actorUserId, long version, Instant occurredAt) implements com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent { }
