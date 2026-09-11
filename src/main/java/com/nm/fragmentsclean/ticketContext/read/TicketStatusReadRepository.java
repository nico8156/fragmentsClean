package com.nm.fragmentsclean.ticketContext.read;

import com.nm.fragmentsclean.ticketContext.read.projections.TicketStatusView;
import java.util.UUID;
import java.util.List;

public interface TicketStatusReadRepository {
	TicketStatusView findById(UUID ticketId); // null si absent
	default TicketStatusView findByIdAndUserId(UUID ticketId, UUID userId) {
        throw new UnsupportedOperationException("Owner-scoped ticket read required");
    }
	List<TicketStatusView> list();
}
