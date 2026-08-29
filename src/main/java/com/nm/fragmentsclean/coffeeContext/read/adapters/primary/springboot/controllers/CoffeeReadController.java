package com.nm.fragmentsclean.coffeeContext.read.adapters.primary.springboot.controllers;


import com.nm.fragmentsclean.coffeeContext.read.GetCoffeeQuery;
import com.nm.fragmentsclean.coffeeContext.read.SearchPublicCoffeesQuery;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.QueryBus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
class CoffeeReadController {

    private final QueryBus querryBus;

    public CoffeeReadController(QueryBus querryBus) {
        this.querryBus = querryBus;
    }

	@GetMapping("/api/coffees")
	public ResponseEntity<List<CoffeeSummaryResponse>> listCoffees(
			@RequestParam(required = false) String query,
			@RequestParam(required = false) String cursor,
			@RequestParam(defaultValue = "50") int limit,
			@RequestHeader(name = "If-None-Match", required = false) String ifNoneMatch) {
		var page = querryBus.dispatch(new SearchPublicCoffeesQuery(query, cursor, limit));
		if (page.etag().equals(ifNoneMatch)) {
			return ResponseEntity.status(304).eTag(page.etag()).build();
		}
		var response = ResponseEntity.ok().eTag(page.etag());
		if (page.nextCursor() != null) response.header("X-Next-Cursor", page.nextCursor());
		return response.body(page.items().stream().map(CoffeeSummaryResponse::from).toList());
	}

	@GetMapping("/api/coffees/{coffeeId}")
	public ResponseEntity<CoffeeSummaryResponse> getCoffee(@PathVariable java.util.UUID coffeeId) {
		return ResponseEntity.of(querryBus.dispatch(new GetCoffeeQuery(coffeeId)).map(CoffeeSummaryResponse::from));
	}
}
