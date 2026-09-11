package com.nm.fragmentsclean.ticketContext.read;

import com.nm.fragmentsclean.ticketContext.read.projections.TicketHistorySlice;

import java.util.UUID;

public interface TicketHistoryReadRepository {
    TicketHistorySlice pageByUserId(UUID userId, Long beforePosition, int limit);
}
