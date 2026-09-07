package com.nm.fragmentsclean.coffeeContext.read.projections;

import java.util.UUID;

public record CoffeePhotoView(
		UUID id,
		UUID coffeeId,
		String photoUri,
		boolean cover,
		int sortOrder) {
	public CoffeePhotoView(UUID id, UUID coffeeId, String photoUri) { this(id, coffeeId, photoUri, false, 0); }
}
