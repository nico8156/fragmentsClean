package com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.jpa;

import com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.jpa.entities.SavedCoffeeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringSavedCoffeeRepository extends JpaRepository<SavedCoffeeJpaEntity, UUID> {
	Optional<SavedCoffeeJpaEntity> findByUserIdAndCoffeeId(UUID userId, UUID coffeeId);
}
