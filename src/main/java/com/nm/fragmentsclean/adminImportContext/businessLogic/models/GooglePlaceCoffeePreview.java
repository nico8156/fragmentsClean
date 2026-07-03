package com.nm.fragmentsclean.adminImportContext.businessLogic.models;

import java.util.List;

public record GooglePlaceCoffeePreview(
		String googlePlaceId,
		String name,
		String formattedAddress,
		String addressLine1,
		String city,
		String postalCode,
		String country,
		double latitude,
		double longitude,
		String phoneNumber,
		String website,
		List<String> openingHours,
		List<GooglePlacePhotoPreview> photos
) {
	public GooglePlaceCoffeePreview {
		if (googlePlaceId == null || googlePlaceId.isBlank()) {
			throw new IllegalArgumentException("googlePlaceId is required");
		}
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("name is required");
		}
		openingHours = openingHours == null ? List.of() : List.copyOf(openingHours);
		photos = photos == null ? List.of() : List.copyOf(photos);
	}
}
