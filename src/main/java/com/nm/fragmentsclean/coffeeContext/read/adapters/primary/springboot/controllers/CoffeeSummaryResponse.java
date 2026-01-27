package com.nm.fragmentsclean.coffeeContext.read.adapters.primary.springboot.controllers;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeSummaryView;

public record CoffeeSummaryResponse(
		UUID id,
		String googleId,
		String name,
		Location location,
		Address address,
		String phoneNumber,
		String website,
		Set<String> tags,
		long version,
		Instant updatedAt) {
	public record Location(double lat, double lon) {
	}

	public record Address(String line1, String city, String postalCode, String country) {
	}

	public static CoffeeSummaryResponse from(CoffeeSummaryView v) {
		return new CoffeeSummaryResponse(
				v.id(),
				v.googleId(),
				v.name(),
				new Location(v.latitude(), v.longitude()),
				new Address(v.addressLine(), v.city(), v.postalCode(), v.country()),
				v.phoneNumber(),
				v.website(),
				v.tags(),
				v.version(),
				v.updatedAt());
	}
}
