package com.nm.fragmentsclean.coffeeContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.nm.fragmentsclean.coffeeContext.read.CoffeeOpeningHoursImportedEventHandler;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeOpeningHoursProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeOpeningHoursView;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeOpeningHoursImportedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.GooglePlaceId;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;

class CoffeeOpeningHoursImportedEventHandlerTest {
	@Test
	void replaces_opening_hours_projection_then_publishes_projection_updated_sync_event() {
		var repository = new RecordingOpeningHoursProjectionRepository();
		var syncPublisher = new RecordingProjectionSyncPublisher();
		var handler = new CoffeeOpeningHoursImportedEventHandler(repository, syncPublisher);
		var event = openingHoursImportedEvent(List.of(
				"Monday: 8:00 AM - 6:00 PM",
				"Tuesday: 8:00 AM - 6:00 PM"));

		handler.handle(event);

		assertThat(repository.replacedCoffeeIds).containsExactly(event.coffeeId().value());
		assertThat(repository.replacedOpeningHours).hasSize(2)
				.extracting(CoffeeOpeningHoursView::weekdayDescription)
				.containsExactly("Monday: 8:00 AM - 6:00 PM", "Tuesday: 8:00 AM - 6:00 PM");
		assertThat(repository.replacedOpeningHours)
				.extracting(CoffeeOpeningHoursView::coffeeId)
				.containsExactly(event.coffeeId().value(), event.coffeeId().value());

		assertThat(syncPublisher.events).hasSize(1);
		ProjectionSyncEvent syncEvent = syncPublisher.events.getFirst();
		assertThat(syncEvent.eventName()).isEqualTo("projection.updated");
		assertThat(syncEvent.projection()).isEqualTo("coffees");
		assertThat(syncEvent.scope()).isEqualTo("entity");
		assertThat(syncEvent.entityId()).isEqualTo(event.coffeeId().value().toString());
		assertThat(syncEvent.version()).isEqualTo(event.version());
		assertThat(syncEvent.changedAt()).isEqualTo(event.occurredAt());
		assertThat(syncEvent.hints()).containsExactly("openingHours");
	}

	@Test
	void replaces_with_empty_list_and_still_publishes_projection_sync() {
		var repository = new RecordingOpeningHoursProjectionRepository();
		var syncPublisher = new RecordingProjectionSyncPublisher();
		var handler = new CoffeeOpeningHoursImportedEventHandler(repository, syncPublisher);
		var event = openingHoursImportedEvent(List.of());

		handler.handle(event);

		assertThat(repository.replacedCoffeeIds).containsExactly(event.coffeeId().value());
		assertThat(repository.replacedOpeningHours).isEmpty();
		assertThat(syncPublisher.events).hasSize(1);
		assertThat(syncPublisher.events.getFirst().hints()).containsExactly("openingHours");
	}

	private static CoffeeOpeningHoursImportedEvent openingHoursImportedEvent(List<String> weekdayDescriptions) {
		return new CoffeeOpeningHoursImportedEvent(
				UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
				UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
				new CoffeeId(UUID.fromString("11111111-1111-1111-1111-111111111111")),
				new GooglePlaceId("places/google-1"),
				weekdayDescriptions,
				42,
				Instant.parse("2026-07-04T10:30:00Z"),
				Instant.parse("2026-07-04T10:29:59Z"));
	}

	private static class RecordingOpeningHoursProjectionRepository implements CoffeeOpeningHoursProjectionRepository {
		private final List<UUID> replacedCoffeeIds = new ArrayList<>();
		private List<CoffeeOpeningHoursView> replacedOpeningHours = List.of();

		@Override
		public void insertSeed(CoffeeOpeningHoursView view) {
		}

		@Override
		public List<CoffeeOpeningHoursView> findAll() {
			return List.of();
		}

		@Override
		public void replaceForCoffee(UUID coffeeId, List<CoffeeOpeningHoursView> openingHours) {
			replacedCoffeeIds.add(coffeeId);
			replacedOpeningHours = List.copyOf(openingHours);
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
