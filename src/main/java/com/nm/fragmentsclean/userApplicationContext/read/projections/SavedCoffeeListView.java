package com.nm.fragmentsclean.userApplicationContext.read.projections;

import java.util.List;

public record SavedCoffeeListView(
		List<SavedCoffeeItemView> items,
		long version,
		String serverTime
) {
}
