package com.nm.fragmentsclean.userApplicationContext.read.projections;

import java.time.Instant;
import java.util.List;

public record SavedCoffeeListView(
		List<SavedCoffeeItemView> items,
		long version,
		Instant serverTime
) {
}
