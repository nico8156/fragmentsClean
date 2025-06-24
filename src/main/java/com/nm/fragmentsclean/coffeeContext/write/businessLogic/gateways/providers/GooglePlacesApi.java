package com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.providers;

import java.util.List;

public interface GooglePlacesApi {
    List<CoffeeFromGoogle> searchCoffeesNearby(double latitude, double longitude);
    List<CoffeeFromGoogle> getRealPhotosFromGoogleApi(List<CoffeeFromGoogle> coffeeFromGoogles);
}
