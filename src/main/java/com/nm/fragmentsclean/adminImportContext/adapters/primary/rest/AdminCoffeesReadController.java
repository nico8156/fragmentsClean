package com.nm.fragmentsclean.adminImportContext.adapters.primary.rest;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nm.fragmentsclean.coffeeContext.read.ListCoffeesQuery;
import com.nm.fragmentsclean.coffeeContext.read.adapters.primary.springboot.controllers.CoffeeSummaryResponse;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.QueryBus;

@RestController
public class AdminCoffeesReadController {
	private final QueryBus queryBus;

	public AdminCoffeesReadController(QueryBus queryBus) {
		this.queryBus = queryBus;
	}

	@GetMapping("/api/admin/coffees")
	public List<CoffeeSummaryResponse> listCoffees() {
		var views = queryBus.dispatch(new ListCoffeesQuery());
		return views.stream()
				.map(CoffeeSummaryResponse::from)
				.toList();
	}
}
