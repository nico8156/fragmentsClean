package com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories;

import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeePhotoView;
import java.util.List;
import java.util.UUID;

public interface CoffeePhotoProjectionRepository {

	// ✅ seed
	void insertSeed(CoffeePhotoView view);

	List<CoffeePhotoView> findAll();

	long count();
}
