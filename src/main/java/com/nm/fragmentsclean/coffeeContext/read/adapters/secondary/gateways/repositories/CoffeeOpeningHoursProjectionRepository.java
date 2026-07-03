package com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories;

import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeOpeningHoursView;
import java.util.List;
import java.util.UUID;

public interface CoffeeOpeningHoursProjectionRepository {

	// ✅ seed
	void insertSeed(CoffeeOpeningHoursView view);

	void replaceForCoffee(UUID coffeeId, List<CoffeeOpeningHoursView> openingHours);

	List<CoffeeOpeningHoursView> findAll();

	long count();
}
