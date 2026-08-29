package com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories;

import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeePhotoView;
import java.util.List;
import java.util.UUID;

public interface CoffeePhotoProjectionRepository {

	// ✅ seed
	void insertSeed(CoffeePhotoView view);

	void replaceForCoffee(UUID coffeeId, List<CoffeePhotoView> photos);

	void append(CoffeePhotoView photo);

	void deletePhoto(UUID coffeeId, UUID photoId);

	void deleteForCoffee(UUID coffeeId);

	List<CoffeePhotoView> findAll();

	default List<CoffeePhotoView> findAll(boolean publishedOnly) {
		return findAll();
	}

	long count();
}
