package com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories;

import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeOpeningHoursView;

import java.util.List;

public interface CoffeeOpeningHoursProjectionRepository {
	List<CoffeeOpeningHoursView> findAll();

	long count();

	void insertSeed(CoffeeOpeningHoursView view);
}
