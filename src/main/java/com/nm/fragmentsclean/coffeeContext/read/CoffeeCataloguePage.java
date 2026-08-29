package com.nm.fragmentsclean.coffeeContext.read;

import java.util.List;

import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeSummaryView;

public record CoffeeCataloguePage(
		List<CoffeeSummaryView> items,
		String nextCursor,
		String etag) {
	public CoffeeCataloguePage {
		items = List.copyOf(items);
	}
}
