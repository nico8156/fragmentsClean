package com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa;

import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa.entities.CoffeeJpaEntity;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.repositories.CoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.Coffee;

import java.util.List;

public class JpaCoffeeRepository implements CoffeeRepository {

    private final SpringCoffeeRepository springCoffeeRepository;

    public JpaCoffeeRepository(SpringCoffeeRepository springCoffeeRepository) {
        this.springCoffeeRepository = springCoffeeRepository;
    }

    @Override
    public void save(Coffee coffee) {
        var coffeeSnapshot = coffee.toSnapshot();
        var coffeeJpaEntity = new CoffeeJpaEntity(
                coffeeSnapshot.id(),
                coffeeSnapshot.googleId(),
                coffeeSnapshot.displayName(),
                coffeeSnapshot.formattedAddress(),
                coffeeSnapshot.nationalPhoneNumber(),
                coffeeSnapshot.websiteUri(),
                coffeeSnapshot.latitude(),
                coffeeSnapshot.longitude()
        );
        springCoffeeRepository.save(coffeeJpaEntity);
    }

    @Override
    public void deleteAll() {

    }

    @Override
    public List<Coffee> findAll() {
        return List.of();
    }
}
