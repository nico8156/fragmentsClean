package com.nm.fragmentsclean.userApplicationContext.read.projections;

import java.util.UUID;

public record SavedCoffeeItemView(
		UUID coffeeId,
		String name,
		String addressLine,
		String city,
		String postalCode,
		String country,
		String savedAt,
		long version
) {
}
