package com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent;

public interface CoffeeProjectionRepository {
    void apply(CoffeeCreatedEvent event);
}
