package com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa;


import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa.entities.CoffeeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringCoffeeRepository extends JpaRepository<CoffeeJpaEntity, UUID> {
}
