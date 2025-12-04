package com.nm.fragmentsclean.coffeeContext.read.adapters.primary.springboot.controllers;


import com.nm.fragmentsclean.coffeeContext.read.ListCoffeesQuery;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.QuerryBus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
class CoffeeReadController {

    private final QuerryBus querryBus;

    public CoffeeReadController(QuerryBus querryBus) {
        this.querryBus = querryBus;
    }

    @GetMapping("/api/coffees")
    public List<CoffeeSummaryResponse> listCoffees() {
        var views = querryBus.dispatch(new ListCoffeesQuery());
        return views.stream()
                .map(CoffeeSummaryResponse::from)
                .toList();
    }
}
