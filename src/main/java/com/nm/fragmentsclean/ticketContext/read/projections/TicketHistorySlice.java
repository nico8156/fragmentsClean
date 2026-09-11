package com.nm.fragmentsclean.ticketContext.read.projections;

import java.util.List;

public record TicketHistorySlice(List<TicketHistoryEntry> entries, Long nextBeforePosition) {
    public TicketHistorySlice {
        entries = entries == null ? List.of() : List.copyOf(entries);
    }
}
