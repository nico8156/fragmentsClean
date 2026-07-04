package com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories;

import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeSummaryView;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent;

import java.util.List;
import java.util.UUID;

public interface CoffeeProjectionRepository {
	void apply(CoffeeCreatedEvent event);

	void deleteByCoffeeId(UUID coffeeId);

	List<CoffeeSummaryView> findAll();

	// ✅ seed : insert direct d'une view (idempotent via ON CONFLICT)
	void insertSeed(CoffeeSummaryView view);

	long count();
}
