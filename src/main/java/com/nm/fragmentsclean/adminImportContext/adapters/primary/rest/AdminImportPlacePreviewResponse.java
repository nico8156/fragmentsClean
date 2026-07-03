package com.nm.fragmentsclean.adminImportContext.adapters.primary.rest;

import java.util.List;

public record AdminImportPlacePreviewResponse(
		String googlePlaceId,
		AdminImportPlaceInfoPreviewResponse info,
		AdminImportOpeningHoursPreviewResponse openingHours,
		List<AdminImportPlacePhotoResponse> photos
) {
}
