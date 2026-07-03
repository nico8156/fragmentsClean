package com.nm.fragmentsclean.adminImportContext.businessLogic.models;

import java.util.UUID;

public record CoffeeCreationResult(
		UUID coffeeId,
		String googlePlaceId,
		GooglePlaceCoffeeImportStatus status
) {
}
