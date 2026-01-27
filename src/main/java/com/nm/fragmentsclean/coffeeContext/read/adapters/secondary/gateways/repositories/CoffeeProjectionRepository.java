package com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories;

import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeSummaryView;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent;

import java.util.List;

public interface CoffeeProjectionRepository {

	/** Projette un event write-side vers le read model */
	void apply(CoffeeCreatedEvent event);

	/** Liste complète (boot front : <1000 ok) */
	List<CoffeeSummaryView> findAll();

	/** Utile pour seed conditionnel */
	long count();

	/** Seed direct read-side (starter pack) : upsert pour être idempotent */
	void upsertSeed(CoffeeSummaryView view);

}
