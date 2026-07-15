package com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.jpa;

import com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.jpa.entities.SavedCoffeeJpaEntity;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.SavedCoffeeRepository;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.SavedCoffee;

import java.util.Optional;
import java.util.UUID;

public class JpaSavedCoffeeRepository implements SavedCoffeeRepository {
	private final SpringSavedCoffeeRepository repository;

	public JpaSavedCoffeeRepository(SpringSavedCoffeeRepository repository) {
		this.repository = repository;
	}

	@Override
	public Optional<SavedCoffee> byId(UUID savedCoffeeId) {
		return repository.findById(savedCoffeeId).map(this::toDomain);
	}

	@Override
	public Optional<SavedCoffee> byUserIdAndCoffeeId(UUID userId, UUID coffeeId) {
		return repository.findByUserIdAndCoffeeId(userId, coffeeId).map(this::toDomain);
	}

	@Override
	public void save(SavedCoffee savedCoffee) {
		repository.save(toJpa(savedCoffee));
	}

	private SavedCoffee toDomain(SavedCoffeeJpaEntity entity) {
		return SavedCoffee.fromSnapshot(new SavedCoffee.SavedCoffeeSnapshot(
				entity.getSavedCoffeeId(),
				entity.getUserId(),
				entity.getCoffeeId(),
				entity.isActive(),
				entity.getUpdatedAt(),
				entity.getVersion()));
	}

	private SavedCoffeeJpaEntity toJpa(SavedCoffee savedCoffee) {
		var snapshot = savedCoffee.toSnapshot();
		return new SavedCoffeeJpaEntity(
				snapshot.savedCoffeeId(),
				snapshot.userId(),
				snapshot.coffeeId(),
				snapshot.active(),
				snapshot.updatedAt(),
				snapshot.version());
	}
}
