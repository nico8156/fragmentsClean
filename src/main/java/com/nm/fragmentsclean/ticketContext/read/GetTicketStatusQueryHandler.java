package com.nm.fragmentsclean.ticketContext.read;

import org.springframework.stereotype.Component;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;
import com.nm.fragmentsclean.ticketContext.read.projections.TicketStatusView;

@Component
public class GetTicketStatusQueryHandler
		implements QueryHandler<GetTicketStatusQuery, TicketStatusView> {

	private final TicketStatusReadRepository repo;

	public GetTicketStatusQueryHandler(TicketStatusReadRepository repo) {
		this.repo = repo;
	}

	@Override
	public TicketStatusView handle(GetTicketStatusQuery query) {
		return repo.findById(query.ticketId());
	}
}
