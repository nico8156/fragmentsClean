package com.nm.fragmentsclean.ticketContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TicketVerificationCompletedEvent(
        UUID eventId,
        UUID commandId,
        UUID ticketId,
        UUID userId,
        Outcome outcome,
        long version,
        Instant occurredAt,
        Instant clientAt,

        Approved approved,   // nullable
        Rejected rejected,   // nullable

        String provider,     // nullable (ex: "openai")
        String providerTraceId // nullable
) implements DomainEvent {

    public enum Outcome {
        APPROVED,
        REJECTED,
        FAILED_RETRYABLE,
        FAILED_FINAL
    }

    public record Approved(
            int amountCents,
            String currency,
            Instant ticketDate,
            String merchantName,
            String merchantAddress,
            String paymentMethod,
            List<Ticket.TicketLineItem> lineItems
    ) {}

    public record Rejected(
            String reasonCode,   // ex: "NOT_A_RECEIPT", "BLURRY", "LOW_CONFIDENCE"
            String message       // debug friendly (nullable)
    ) {}
}
