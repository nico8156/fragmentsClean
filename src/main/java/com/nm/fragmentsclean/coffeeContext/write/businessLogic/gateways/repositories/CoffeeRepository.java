package com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.repositories;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.Coffee;

import java.util.List;

public interface CoffeeRepository {
    void save(Coffee coffee);
    void deleteAll();
    List<Coffee> findAll();
}
