package com.nm.fragmentsclean.ticketContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record TicketAccountDataErasedEvent(
    UUID eventId, UUID requestId, UUID userId, String context, Instant occurredAt)
    implements DomainEvent {}
