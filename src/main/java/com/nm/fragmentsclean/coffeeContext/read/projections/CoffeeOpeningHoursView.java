package com.nm.fragmentsclean.coffeeContext.read.projections;

import java.util.UUID;

public record CoffeeOpeningHoursView(
		UUID id,
		UUID coffeeId,
		String weekdayDescription) {
}
