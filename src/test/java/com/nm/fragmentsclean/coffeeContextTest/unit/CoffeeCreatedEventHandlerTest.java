package com.nm.fragmentsclean.coffeeContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.nm.fragmentsclean.coffeeContext.read.CoffeeCreatedEventHandler;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeSummaryView;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.Address;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeName;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.GeoPoint;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.GooglePlaceId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.PhoneNumber;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.Tag;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.WebsiteUrl;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;

class CoffeeCreatedEventHandlerTest {
	@Test
	void applies_projection_then_publishes_projection_updated_sync_event() {
		var projectionRepository = new RecordingCoffeeProjectionRepository();
		var syncPublisher = new RecordingProjectionSyncPublisher();
		var handler = new CoffeeCreatedEventHandler(projectionRepository, syncPublisher);
		var event = coffeeCreatedEvent();

		handler.handle(event);

		assertThat(projectionRepository.appliedEvents).containsExactly(event);
		assertThat(syncPublisher.events).hasSize(1);
		ProjectionSyncEvent syncEvent = syncPublisher.events.getFirst();
		assertThat(syncEvent.eventName()).isEqualTo("projection.updated");
		assertThat(syncEvent.projection()).isEqualTo("coffees");
		assertThat(syncEvent.scope()).isEqualTo("entity");
		assertThat(syncEvent.entityId()).isEqualTo(event.coffeeId().value().toString());
		assertThat(syncEvent.version()).isEqualTo((long) event.version());
		assertThat(syncEvent.changedAt()).isEqualTo(event.occurredAt());
		assertThat(syncEvent.hints()).containsExactly("summary");
	}

	@Test
	void ignores_stale_event_without_publishing_projection_sync() {
		var projectionRepository = new RecordingCoffeeProjectionRepository();
		projectionRepository.ignoreMutations = true;
		var syncPublisher = new RecordingProjectionSyncPublisher();
		var handler = new CoffeeCreatedEventHandler(projectionRepository, syncPublisher);

		handler.handle(coffeeCreatedEvent());

		assertThat(syncPublisher.events).isEmpty();
	}

	private CoffeeCreatedEvent coffeeCreatedEvent() {
		return new CoffeeCreatedEvent(
				UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
				UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
				new CoffeeId(UUID.fromString("11111111-1111-1111-1111-111111111111")),
				new GooglePlaceId("google-place-1"),
				new CoffeeName("Fragments Cafe"),
				new Address("1 rue Example", "Rennes", "35000", "FR"),
				new GeoPoint(48.11, -1.67),
				new PhoneNumber("0200000000"),
				new WebsiteUrl("https://example.com"),
				List.of(new Tag("google-places")),
				7,
				Instant.parse("2026-07-03T10:00:00Z"),
				Instant.parse("2026-07-03T09:59:59Z"));
	}

	private static class RecordingCoffeeProjectionRepository implements CoffeeProjectionRepository {
		private final List<CoffeeCreatedEvent> appliedEvents = new ArrayList<>();
		private boolean ignoreMutations;

		@Override
		public CoffeeProjectionMutation applyIfNewer(CoffeeCreatedEvent event) {
			if (ignoreMutations) {
				return CoffeeProjectionMutation.ignored(event.version() + 1L, event.occurredAt().plusSeconds(1));
			}
			return CoffeeProjectionRepository.super.applyIfNewer(event);
		}

		@Override
		public void apply(CoffeeCreatedEvent event) {
			appliedEvents.add(event);
		}

		@Override
		public void deleteByCoffeeId(UUID coffeeId) {
		}

		@Override
		public List<CoffeeSummaryView> findAll() {
			return List.of();
		}

		@Override
		public void insertSeed(CoffeeSummaryView view) {
		}

		@Override
		public long count() {
			return 0;
		}
	}

	private static class RecordingProjectionSyncPublisher implements ProjectionSyncPublisher {
		private final List<ProjectionSyncEvent> events = new ArrayList<>();

		@Override
		public void publish(ProjectionSyncEvent event) {
			events.add(event);
		}
	}
}
