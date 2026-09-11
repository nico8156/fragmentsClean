package com.nm.fragmentsclean.ticketContext.read.projections;

import java.util.List;

public record TicketHistoryPageView(List<TicketHistoryItemView> items, String nextCursor) {
    public TicketHistoryPageView {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
