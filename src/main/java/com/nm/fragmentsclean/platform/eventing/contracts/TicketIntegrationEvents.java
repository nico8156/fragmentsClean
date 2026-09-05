package com.nm.fragmentsclean.platform.eventing.contracts;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class TicketIntegrationEvents {
    private TicketIntegrationEvents() { }

    public record VerifyAccepted(UUID eventId, UUID commandId, UUID ticketId, UUID userId,
                                 String ocrText, String imageRef, String status, long version,
                                 Instant occurredAt, Instant clientAt) { }

    public record VerificationCompleted(UUID eventId, UUID commandId, UUID ticketId, UUID userId,
                                        String outcome, long version, Instant occurredAt, Instant clientAt,
                                        Approved approved, Rejected rejected, String provider, String providerTraceId) { }

    public record Approved(int amountCents, String currency, Instant ticketDate, String merchantName,
                           String merchantAddress, String paymentMethod, List<LineItem> lineItems) {
        public Approved { lineItems = lineItems == null ? List.of() : List.copyOf(lineItems); }
    }
    public record LineItem(String label, Integer quantity, Integer amountCents) { }
    public record Rejected(String reasonCode, String message) { }

    public record AdminUpdated(UUID eventId, UUID commandId, UUID ticketId, UUID userId, String status,
                               String ocrText, String imageRef, Integer amountCents, String currency,
                               Instant ticketDate, String merchantName, String merchantAddress,
                               String paymentMethod, String rejectionReason, long version,
                               UUID actorUserId, Instant occurredAt) { }
    public record AdminDeleted(UUID eventId, UUID commandId, UUID ticketId, UUID userId,
                               UUID actorUserId, long version, Instant occurredAt) { }
}
