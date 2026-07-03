package com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways;

public interface CoffeeGooglePlaceLookupPort {
	boolean existsByGooglePlaceId(String googlePlaceId);
}
