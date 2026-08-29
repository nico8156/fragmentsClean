package com.nm.fragmentsclean.coffeeContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.nm.fragmentsclean.coffeeContext.read.GetCoffeeQuery;
import com.nm.fragmentsclean.coffeeContext.read.GetCoffeeQueryHandler;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeSummaryView;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent;

class GetCoffeeQueryHandlerTest {

	@Test
	void requests_only_the_published_projection_for_the_requested_id() {
		var repository = new RecordingRepository();
		var handler = new GetCoffeeQueryHandler(repository);
		UUID coffeeId = UUID.fromString("11111111-1111-1111-1111-111111111111");

		Optional<CoffeeSummaryView> result = handler.handle(new GetCoffeeQuery(coffeeId));

		assertThat(result).isEmpty();
		assertThat(repository.requestedCoffeeId).isEqualTo(coffeeId);
		assertThat(repository.publishedOnly).isTrue();
	}

	private static class RecordingRepository implements CoffeeProjectionRepository {
		private UUID requestedCoffeeId;
		private boolean publishedOnly;

		@Override
		public Optional<CoffeeSummaryView> findById(UUID coffeeId, boolean publishedOnly) {
			this.requestedCoffeeId = coffeeId;
			this.publishedOnly = publishedOnly;
			return Optional.empty();
		}

		@Override public void apply(CoffeeCreatedEvent event) { }
		@Override public void deleteByCoffeeId(UUID coffeeId) { }
		@Override public List<CoffeeSummaryView> findAll() { return List.of(); }
		@Override public void insertSeed(CoffeeSummaryView view) { }
		@Override public long count() { return 0; }
	}
}
