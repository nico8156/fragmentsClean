package com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories;

import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeSummaryView;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent;

import java.util.List;

public interface CoffeeProjectionRepository {
    void apply(CoffeeCreatedEvent event);
    List<CoffeeSummaryView> findAll();

}
