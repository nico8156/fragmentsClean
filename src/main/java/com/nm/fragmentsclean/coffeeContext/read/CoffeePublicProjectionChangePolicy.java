package com.nm.fragmentsclean.coffeeContext.read;

import java.util.UUID;

import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeProjectionRepository;

/** Decides whether a coffee projection mutation changes the public mobile catalogue. */
public final class CoffeePublicProjectionChangePolicy {
	private final CoffeeProjectionRepository coffeeProjectionRepository;

	public CoffeePublicProjectionChangePolicy(CoffeeProjectionRepository coffeeProjectionRepository) {
		this.coffeeProjectionRepository = coffeeProjectionRepository;
	}

	public boolean isPubliclyVisible(UUID coffeeId) {
		return coffeeProjectionRepository.isPublished(coffeeId);
	}
}
