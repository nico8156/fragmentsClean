package com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.GooglePlacePhoto;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.ImportedCoffeePhoto;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.GooglePlaceId;

public interface CoffeePhotoStorage {
	ImportedCoffeePhoto store(CoffeeId coffeeId, GooglePlaceId googlePlaceId, GooglePlacePhoto photo);
	default boolean canDeletePhoto(CoffeeId coffeeId, java.util.UUID photoId, String storedReference) { return false; }
	default void deletePhoto(CoffeeId coffeeId, java.util.UUID photoId, String storedReference) { throw new UnsupportedOperationException("Targeted coffee photo deletion is unsupported"); }
	default void deleteForCoffee(CoffeeId coffeeId) { }
}
