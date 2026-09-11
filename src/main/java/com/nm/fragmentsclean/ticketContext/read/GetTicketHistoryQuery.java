package com.nm.fragmentsclean.ticketContext.read;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.Query;
import com.nm.fragmentsclean.ticketContext.read.projections.TicketHistoryPageView;

import java.util.UUID;

public record GetTicketHistoryQuery(UUID requesterId, String cursor, int limit)
        implements Query<TicketHistoryPageView> {
}
