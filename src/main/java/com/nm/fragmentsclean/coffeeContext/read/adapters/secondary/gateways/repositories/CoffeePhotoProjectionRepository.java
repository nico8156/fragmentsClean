package com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories;

import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeePhotoView;

import java.util.List;

public interface CoffeePhotoProjectionRepository {
	List<CoffeePhotoView> findAll();

	long count();

	void insertSeed(CoffeePhotoView view);
}
