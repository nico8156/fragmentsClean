package com.nm.fragmentsclean.adminImportContext.adapters.primary.rest;

import java.util.List;

public record AdminImportPlacePreviewResponse(
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
		List<AdminImportPlacePhotoResponse> photos
) {
}
