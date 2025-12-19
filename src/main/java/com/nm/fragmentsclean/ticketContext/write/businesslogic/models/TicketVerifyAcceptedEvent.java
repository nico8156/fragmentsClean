package com.nm.fragmentsclean.ticketContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record TicketVerifyAcceptedEvent(
        UUID eventId,
        UUID commandId,
        UUID ticketId,
        UUID userId,
        String ocrText,
        String imageRef,
        Ticket.TicketStatus status,
        long version,
        Instant occurredAt,
        Instant clientAt
) implements DomainEvent {
}
