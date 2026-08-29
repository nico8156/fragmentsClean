package com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories;

import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeSummaryView;
import com.nm.fragmentsclean.coffeeContext.read.CoffeeCataloguePage;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent;

import java.util.List;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface CoffeeProjectionRepository {
	void apply(CoffeeCreatedEvent event);

	default CoffeeProjectionMutation applyIfNewer(CoffeeCreatedEvent event) {
		apply(event);
		return CoffeeProjectionMutation.applied(event.version(), event.occurredAt());
	}

	default void apply(CoffeeSummaryView view) {
		insertSeed(view);
	}

	default CoffeeProjectionMutation applyIfNewer(CoffeeSummaryView view) {
		apply(view);
		return CoffeeProjectionMutation.applied(view.version(), view.updatedAt());
	}

	void deleteByCoffeeId(UUID coffeeId);

	default CoffeeProjectionMutation deleteIfNewer(UUID coffeeId, long version, Instant changedAt) {
		deleteByCoffeeId(coffeeId);
		return CoffeeProjectionMutation.applied(version, changedAt);
	}

	default void markArchived(UUID coffeeId, long version, java.time.Instant updatedAt) { }
	default void markPublished(UUID coffeeId, long version, java.time.Instant updatedAt) { }

	default CoffeeProjectionMutation markArchivedIfNewer(UUID coffeeId, long version, Instant changedAt) {
		markArchived(coffeeId, version, changedAt);
		return CoffeeProjectionMutation.applied(version, changedAt);
	}

	default CoffeeProjectionMutation markPublishedIfNewer(UUID coffeeId, long version, Instant changedAt) {
		markPublished(coffeeId, version, changedAt);
		return CoffeeProjectionMutation.applied(version, changedAt);
	}

	List<CoffeeSummaryView> findAll();

	default Optional<CoffeeSummaryView> findById(UUID coffeeId, boolean publishedOnly) {
		return findAll(publishedOnly).stream().filter(view -> view.id().equals(coffeeId)).findFirst();
	}

	default List<CoffeeSummaryView> findAll(boolean publishedOnly) { return findAll(); }

	default boolean isPublished(UUID coffeeId) {
		return findById(coffeeId, true).isPresent();
	}

	default CoffeeCataloguePage searchPublished(String search, String cursor, int limit) {
		if (cursor != null) throw new IllegalArgumentException("cursor is not supported by this repository");
		List<CoffeeSummaryView> allMatches = findAll(true).stream()
				.filter(view -> matches(view, search))
				.toList();
		List<CoffeeSummaryView> page = allMatches.stream().limit(limit).toList();
		return new CoffeeCataloguePage(page, null, CoffeeCatalogueEtag.from(search, allMatches));
	}

	private static boolean matches(CoffeeSummaryView view, String search) {
		if (search == null) return true;
		String expected = search.toLowerCase(java.util.Locale.ROOT);
		return java.util.stream.Stream.of(view.name(), view.city(), view.postalCode(), view.addressLine())
				.filter(java.util.Objects::nonNull)
				.map(value -> value.toLowerCase(java.util.Locale.ROOT))
				.anyMatch(value -> value.contains(expected));
	}

	// ✅ seed : insert direct d'une view (idempotent via ON CONFLICT)
	void insertSeed(CoffeeSummaryView view);

	long count();

	record CoffeeProjectionMutation(boolean applied, long version, Instant changedAt) {
		public static CoffeeProjectionMutation applied(long version, Instant changedAt) {
			return new CoffeeProjectionMutation(true, version, changedAt);
		}

		public static CoffeeProjectionMutation ignored(long version, Instant changedAt) {
			return new CoffeeProjectionMutation(false, version, changedAt);
		}
	}
}
