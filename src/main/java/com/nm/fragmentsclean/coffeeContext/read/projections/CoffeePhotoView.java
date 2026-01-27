package com.nm.fragmentsclean.coffeeContext.read.projections;

import java.util.UUID;

public record CoffeePhotoView(
		UUID id,
		UUID coffeeId,
		String photoUri) {
}
