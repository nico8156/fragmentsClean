package com.nm.fragmentsclean.adminImportContext.businessLogic.ports;

import java.util.List;
import java.util.Optional;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.GooglePlaceCoffeePreview;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.GooglePlaceSearchResult;

public interface GooglePlacesGateway {
	List<GooglePlaceSearchResult> searchCoffeePlaces(String query);

	Optional<GooglePlaceCoffeePreview> findCoffeePreview(String googlePlaceId);
}
