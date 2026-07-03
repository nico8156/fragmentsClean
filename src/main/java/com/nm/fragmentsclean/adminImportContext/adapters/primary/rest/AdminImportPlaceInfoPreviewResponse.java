package com.nm.fragmentsclean.adminImportContext.adapters.primary.rest;

public record AdminImportPlaceInfoPreviewResponse(
		String name,
		String formattedAddress,
		double latitude,
		double longitude,
		String phoneNumber,
		String website,
		String googleMapsUri,
		String businessStatus
) {
}
