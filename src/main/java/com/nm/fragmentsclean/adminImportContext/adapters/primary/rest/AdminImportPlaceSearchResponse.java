package com.nm.fragmentsclean.adminImportContext.adapters.primary.rest;

public record AdminImportPlaceSearchResponse(
		String googlePlaceId,
		String name,
		String formattedAddress,
		double latitude,
		double longitude
) {
}
