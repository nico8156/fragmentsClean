package com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa;

import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.jpa.entities.CoffeeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringCoffeeRepository extends JpaRepository<CoffeeJpaEntity, UUID> {

    @org.springframework.data.jpa.repository.Query(value="SELECT id FROM coffees WHERE id=:id FOR UPDATE",nativeQuery=true)
    @org.springframework.transaction.annotation.Transactional
    java.util.Optional<UUID> lockSourceById(@org.springframework.data.repository.query.Param("id") UUID id);

    boolean existsByGooglePlaceId(String googlePlaceId);
}
