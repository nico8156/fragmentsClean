package com.nm.fragmentsclean.coffeeContext.read.adapters.primary.springboot.controllers;


import com.nm.fragmentsclean.coffeeContext.read.ListCoffeesQuery;
import com.nm.fragmentsclean.coffeeContext.read.GetCoffeeQuery;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.QueryBus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
class CoffeeReadController {

    private final QueryBus querryBus;

    public CoffeeReadController(QueryBus querryBus) {
        this.querryBus = querryBus;
    }

    @GetMapping("/api/coffees")
    public List<CoffeeSummaryResponse> listCoffees() {
        var views = querryBus.dispatch(new ListCoffeesQuery());
        return views.stream()
                .map(CoffeeSummaryResponse::from)
                .toList();
    }

	@GetMapping("/api/coffees/{coffeeId}")
	public ResponseEntity<CoffeeSummaryResponse> getCoffee(@PathVariable java.util.UUID coffeeId) {
		return ResponseEntity.of(querryBus.dispatch(new GetCoffeeQuery(coffeeId)).map(CoffeeSummaryResponse::from));
	}
}
