package com.nm.fragmentsclean.adminImportContext.businessLogic.usecases;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.GooglePlaceCoffeePreview;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.GooglePlacesGateway;

public class PreviewGooglePlaceCoffee {
	private final GooglePlacesGateway googlePlacesGateway;

	public PreviewGooglePlaceCoffee(GooglePlacesGateway googlePlacesGateway) {
		this.googlePlacesGateway = googlePlacesGateway;
	}

	public GooglePlaceCoffeePreview execute(String googlePlaceId) {
		if (googlePlaceId == null || googlePlaceId.isBlank()) {
			throw new IllegalArgumentException("googlePlaceId is required");
		}
		return googlePlacesGateway.findCoffeePreview(googlePlaceId.trim())
				.orElseThrow(() -> new GooglePlaceNotFoundException(googlePlaceId));
	}
}
