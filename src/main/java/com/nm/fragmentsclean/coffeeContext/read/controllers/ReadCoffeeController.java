package com.nm.fragmentsclean.coffeeContext.read.controllers;

import com.nm.fragmentsclean.coffeeContext.read.CoffeeListView;
import com.nm.fragmentsclean.coffeeContext.read.GetCoffeesQuery;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.QuerryBus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/coffees")
public class ReadCoffeeController {

    private final QuerryBus queryBus;

    public ReadCoffeeController(QuerryBus queryBus) {
        this.queryBus = queryBus;
    }

    @RequestMapping
    public ResponseEntity<CoffeeListView> getAllCoffees() {
        var query = new GetCoffeesQuery();
        return ResponseEntity.ok(queryBus.dispatch(query));
    }
}
