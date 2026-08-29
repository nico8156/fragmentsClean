package com.nm.fragmentsclean.coffeeContextTest.support;

import java.util.List;
import java.util.UUID;

import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeSummaryView;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent;

public final class PublishedCoffeeProjectionRepository implements CoffeeProjectionRepository {
	@Override public boolean isPublished(UUID coffeeId) { return true; }
	@Override public void apply(CoffeeCreatedEvent event) { }
	@Override public void deleteByCoffeeId(UUID coffeeId) { }
	@Override public List<CoffeeSummaryView> findAll() { return List.of(); }
	@Override public void insertSeed(CoffeeSummaryView view) { }
	@Override public long count() { return 0; }
}
