package com.nm.fragmentsclean.ticketContext.read.adapters.primary.springboot.controllers;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.QueryBus;
import com.nm.fragmentsclean.ticketContext.read.GetTicketStatusQuery;
import com.nm.fragmentsclean.ticketContext.read.projections.TicketStatusView;

@RestController
@RequestMapping("/api/tickets")
public class ReadTicketController {

	private final QueryBus queryBus;

	public ReadTicketController(QueryBus queryBus) {
		this.queryBus = queryBus;
	}

	@GetMapping("/{ticketId}/status")
	public ResponseEntity<TicketStatusView> getStatus(@PathVariable UUID ticketId) {
		TicketStatusView view = queryBus.dispatch(new GetTicketStatusQuery(ticketId));
		if (view == null)
			return ResponseEntity.notFound().build();
		return ResponseEntity.ok(view);
	}
}
