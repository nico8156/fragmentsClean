package com.nm.fragmentsclean.adminImportContext.businessLogic.models;

import java.util.UUID;

public record ImportedGooglePlaceCoffee(
		UUID commandId,
		UUID coffeeId,
		String googlePlaceId
) {
}
