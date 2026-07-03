package com.nm.fragmentsclean.adminImportContext.adapters.primary.rest;

import java.util.UUID;

public record AdminImportPlaceImportResponse(
		UUID commandId,
		UUID coffeeId,
		String googlePlaceId
) {
}
