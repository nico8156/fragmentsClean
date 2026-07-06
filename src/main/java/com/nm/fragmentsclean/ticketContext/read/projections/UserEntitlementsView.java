package com.nm.fragmentsclean.ticketContext.read.projections;

import java.time.Instant;
import java.util.UUID;

public record UserEntitlementsView(
        UUID userId,
        int confirmedTickets,
        long version,
        Instant updatedAt) {
}
