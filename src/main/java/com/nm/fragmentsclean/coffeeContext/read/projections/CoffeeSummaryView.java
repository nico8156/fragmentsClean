
package com.nm.fragmentsclean.coffeeContext.read.projections;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record CoffeeSummaryView(
		UUID id,
		String googleId,
		String name,
		double latitude,
		double longitude,
		String addressLine,
		String city,
		String postalCode,
		String country,
		String phoneNumber,
		String website,
		Set<String> tags,
		long version,
		Instant updatedAt) {
}
