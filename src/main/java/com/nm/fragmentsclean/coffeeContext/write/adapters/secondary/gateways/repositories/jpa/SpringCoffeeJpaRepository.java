package com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa;

import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa.entities.CoffeeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringCoffeeJpaRepository extends JpaRepository<CoffeeJpaEntity, UUID> {

    boolean existsByGooglePlaceId(String googlePlaceId);
}
