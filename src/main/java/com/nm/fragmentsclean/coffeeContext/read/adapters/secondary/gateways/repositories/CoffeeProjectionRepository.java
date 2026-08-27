package com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories;

import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeSummaryView;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent;

import java.util.List;
import java.util.UUID;

public interface CoffeeProjectionRepository {
	void apply(CoffeeCreatedEvent event);

	default void apply(CoffeeSummaryView view) {
		insertSeed(view);
	}

	void deleteByCoffeeId(UUID coffeeId);

	void markArchived(UUID coffeeId, long version, java.time.Instant updatedAt);
	void markPublished(UUID coffeeId, long version, java.time.Instant updatedAt);

	List<CoffeeSummaryView> findAll(boolean publishedOnly);

	default List<CoffeeSummaryView> findAll() { return findAll(true); }

	// ✅ seed : insert direct d'une view (idempotent via ON CONFLICT)
	void insertSeed(CoffeeSummaryView view);

	long count();
}
