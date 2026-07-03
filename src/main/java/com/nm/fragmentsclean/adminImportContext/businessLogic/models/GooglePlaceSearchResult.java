package com.nm.fragmentsclean.adminImportContext.businessLogic.models;

public record GooglePlaceSearchResult(
		String googlePlaceId,
		String name,
		String formattedAddress,
		double latitude,
		double longitude
) {
}
