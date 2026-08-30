package com.nm.fragmentsclean.coffeeContext.read;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.Query;

public record SearchPublicCoffeesQuery(String search, String cursor, int limit)
		implements Query<CoffeeCataloguePage> {
	public static final int DEFAULT_LIMIT = 50;
	public static final int MAX_LIMIT = 100;

	public SearchPublicCoffeesQuery {
		search = normalize(search);
		cursor = normalize(cursor);
		if (limit < 1 || limit > MAX_LIMIT) {
			throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
		}
	}

	private static String normalize(String value) {
		if (value == null) return null;
		String normalized = value.trim();
		return normalized.isEmpty() ? null : normalized;
	}
}
