package com.nm.fragmentsclean.adminImportContext.adapters.primary.rest;

import java.util.List;

public record AdminImportPlacePhotoResponse(
		String name,
		Integer widthPx,
		Integer heightPx,
		List<String> authorAttributions
) {
}
