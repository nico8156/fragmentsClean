package com.nm.fragmentsclean.ticketContext.read;

import java.util.UUID;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.Query;
import com.nm.fragmentsclean.ticketContext.read.projections.TicketStatusView;

public record GetTicketStatusQuery(UUID ticketId, UUID userId) implements Query<TicketStatusView> {
    public GetTicketStatusQuery {
        java.util.Objects.requireNonNull(userId, "Authenticated user required");
    }
}
