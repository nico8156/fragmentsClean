package com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.fakes;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.repositories.CoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.Coffee;

import java.util.ArrayList;
import java.util.List;

public class FakeCoffeeRepository implements CoffeeRepository {
    public List<Coffee> coffeeList = new ArrayList<>();

    @Override
    public void save(Coffee coffee) {coffeeList.add(coffee);}

    @Override
    public void deleteAll() {
        coffeeList.clear();
    }

    @Override
    public List<Coffee> findAll() {
        return coffeeList;
    }
}
