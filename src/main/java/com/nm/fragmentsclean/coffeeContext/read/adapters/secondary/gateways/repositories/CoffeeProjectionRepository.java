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

	default void markArchived(UUID coffeeId, long version, java.time.Instant updatedAt) { }
	default void markPublished(UUID coffeeId, long version, java.time.Instant updatedAt) { }

	List<CoffeeSummaryView> findAll();

	default List<CoffeeSummaryView> findAll(boolean publishedOnly) { return findAll(); }

	// ✅ seed : insert direct d'une view (idempotent via ON CONFLICT)
	void insertSeed(CoffeeSummaryView view);

	long count();
}
