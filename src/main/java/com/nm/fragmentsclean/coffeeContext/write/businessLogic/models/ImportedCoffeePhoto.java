package com.nm.fragmentsclean.coffeeContext.write.businessLogic.models;

import java.util.Objects;
import java.util.UUID;

public record ImportedCoffeePhoto(UUID photoId, String photoUri) {
	public ImportedCoffeePhoto {
		Objects.requireNonNull(photoId, "photoId is required");
		if (photoUri == null || photoUri.isBlank()) {
			throw new IllegalArgumentException("photoUri is required");
		}
		photoUri = photoUri.trim();
	}
}
