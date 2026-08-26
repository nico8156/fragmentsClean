package com.nm.fragmentsclean.coffeeContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.nm.fragmentsclean.coffeeContext.read.CoffeeCreatedIntegrationEventHandler;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeProjectionSource;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeSummaryView;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.CoffeeCreatedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;

class CoffeeCreatedIntegrationEventHandlerTest {
	@Test
	void projects_the_authoritative_coffee_snapshot_from_a_primitive_sqs_contract() {
		var view = coffeeView();
		var source = (CoffeeProjectionSource) coffeeId -> Optional.of(view);
		var projectionRepository = new RecordingProjectionRepository();
		var syncPublisher = new RecordingProjectionSyncPublisher();
		var handler = new CoffeeCreatedIntegrationEventHandler(source, projectionRepository, syncPublisher);

		handler.handle(new CoffeeCreatedIntegrationEvent(
				UUID.randomUUID(), UUID.randomUUID(), view.id(), view.name(), view.addressLine(), view.city(),
				view.postalCode(), view.country(), (int) view.version(), view.updatedAt()));

		assertThat(projectionRepository.appliedViews).containsExactly(view);
		assertThat(syncPublisher.events).singleElement().satisfies(event -> {
			assertThat(event.projection()).isEqualTo("coffees");
			assertThat(event.entityId()).isEqualTo(view.id().toString());
			assertThat(event.version()).isEqualTo(view.version());
		});
	}

	private CoffeeSummaryView coffeeView() {
		return new CoffeeSummaryView(UUID.fromString("11111111-1111-1111-1111-111111111111"), "google-place-1",
				"Fragments Cafe", 48.11, -1.67, "1 rue Example", "Rennes", "35000", "FR",
				"0200000000", "https://example.com", Set.of("google-places"), 7,
				Instant.parse("2026-08-26T20:00:00Z"));
	}

	private static class RecordingProjectionRepository implements CoffeeProjectionRepository {
		private final List<CoffeeSummaryView> appliedViews = new ArrayList<>();
		@Override public void apply(CoffeeCreatedEvent event) { }
		@Override public void apply(CoffeeSummaryView view) { appliedViews.add(view); }
		@Override public void deleteByCoffeeId(UUID coffeeId) { }
		@Override public List<CoffeeSummaryView> findAll() { return List.of(); }
		@Override public void insertSeed(CoffeeSummaryView view) { }
		@Override public long count() { return 0; }
	}

	private static class RecordingProjectionSyncPublisher implements ProjectionSyncPublisher {
		private final List<ProjectionSyncEvent> events = new ArrayList<>();
		@Override public void publish(ProjectionSyncEvent event) { events.add(event); }
	}
}
