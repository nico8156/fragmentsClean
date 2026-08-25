package com.nm.fragmentsclean.ticketContext.read;

import com.nm.fragmentsclean.ticketContext.read.projections.TicketStatusView;
import java.util.UUID;
import java.util.List;

public interface TicketStatusReadRepository {
	TicketStatusView findById(UUID ticketId); // null si absent
	List<TicketStatusView> list();
}
