package com.nm.fragmentsclean.ticketContext.read.projections;

import java.time.Instant;
import java.util.UUID;

public record TicketHistoryItemView(
        UUID ticketId,
        String status,
        String outcome,
        Integer amountCents,
        String currency,
        Instant ticketDate,
        String merchantName,
        String merchantAddress,
        String rejectionReason,
        long version,
        Instant occurredAt) {
}
