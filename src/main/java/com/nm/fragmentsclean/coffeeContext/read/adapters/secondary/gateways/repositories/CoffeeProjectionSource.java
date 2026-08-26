package com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories;

import java.util.Optional;
import java.util.UUID;

import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeSummaryView;

/** Read-side snapshot port owned by the coffee context. */
public interface CoffeeProjectionSource {
	Optional<CoffeeSummaryView> findByCoffeeId(UUID coffeeId);
}
