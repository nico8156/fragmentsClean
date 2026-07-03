package com.nm.fragmentsclean.adminImportContext.businessLogic.usecases;

import java.util.List;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.GooglePlaceSearchResult;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.GooglePlacesGateway;

public class SearchGooglePlacesForCoffee {
	private final GooglePlacesGateway googlePlacesGateway;

	public SearchGooglePlacesForCoffee(GooglePlacesGateway googlePlacesGateway) {
		this.googlePlacesGateway = googlePlacesGateway;
	}

	public List<GooglePlaceSearchResult> execute(String query) {
		if (query == null || query.isBlank()) {
			throw new IllegalArgumentException("query is required");
		}
		return googlePlacesGateway.searchCoffeePlaces(query.trim());
	}
}
