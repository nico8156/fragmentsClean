package com.nm.fragmentsclean.coffeeContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.nm.fragmentsclean.coffeeContext.read.CoffeeArchivedEventHandler;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeOpeningHoursProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeePhotoProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeOpeningHoursView;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeePhotoView;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeSummaryView;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeArchivedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;

class CoffeeArchivedEventHandlerTest {
	@Test
	void marks_coffee_archived_without_removing_studio_details_then_publishes_projection_updated_sync_event() {
		var summaryRepository = new RecordingCoffeeProjectionRepository();
		var photoRepository = new RecordingPhotoProjectionRepository();
		var openingHoursRepository = new RecordingOpeningHoursProjectionRepository();
		var syncPublisher = new RecordingProjectionSyncPublisher();
		var handler = new CoffeeArchivedEventHandler(
				summaryRepository,
				photoRepository,
				openingHoursRepository,
				syncPublisher);
		var event = coffeeArchivedEvent();

		handler.handle(event);

		assertThat(summaryRepository.archived).containsExactly(
				new ArchivedCoffee(event.coffeeId().value(), event.version(), event.occurredAt()));
		assertThat(photoRepository.deletedCoffeeIds).isEmpty();
		assertThat(openingHoursRepository.deletedCoffeeIds).isEmpty();
		assertThat(summaryRepository.deletedCoffeeIds).isEmpty();
		assertThat(syncPublisher.events).hasSize(1);
		ProjectionSyncEvent syncEvent = syncPublisher.events.getFirst();
		assertThat(syncEvent.eventName()).isEqualTo("projection.updated");
		assertThat(syncEvent.projection()).isEqualTo("coffees");
		assertThat(syncEvent.scope()).isEqualTo("entity");
		assertThat(syncEvent.entityId()).isEqualTo(event.coffeeId().value().toString());
		assertThat(syncEvent.version()).isEqualTo((long) event.version());
		assertThat(syncEvent.changedAt()).isEqualTo(event.occurredAt());
		assertThat(syncEvent.hints()).containsExactly("archived", "summary");
	}

	private CoffeeArchivedEvent coffeeArchivedEvent() {
		return new CoffeeArchivedEvent(
				UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
				UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
				new CoffeeId(UUID.fromString("11111111-1111-1111-1111-111111111111")),
				8,
				Instant.parse("2026-07-04T11:00:00Z"),
				Instant.parse("2026-07-04T10:59:59Z"));
	}

	private static class RecordingCoffeeProjectionRepository implements CoffeeProjectionRepository {
		private final List<UUID> deletedCoffeeIds = new ArrayList<>();
		private final List<ArchivedCoffee> archived = new ArrayList<>();

		@Override
		public void apply(CoffeeCreatedEvent event) {
		}

		@Override
		public void deleteByCoffeeId(UUID coffeeId) {
			deletedCoffeeIds.add(coffeeId);
		}

		@Override
		public void markArchived(UUID coffeeId, long version, Instant updatedAt) {
			archived.add(new ArchivedCoffee(coffeeId, version, updatedAt));
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

	private record ArchivedCoffee(UUID coffeeId, long version, Instant updatedAt) {
	}

	private static class RecordingPhotoProjectionRepository implements CoffeePhotoProjectionRepository {
		private final List<UUID> deletedCoffeeIds = new ArrayList<>();

		@Override
		public void insertSeed(CoffeePhotoView view) {
		}

		@Override
		public void replaceForCoffee(UUID coffeeId, List<CoffeePhotoView> photos) {
		}

		@Override
		public void append(CoffeePhotoView photo) {
		}

		@Override
		public void deletePhoto(UUID coffeeId, UUID photoId) {
		}

		@Override
		public void deleteForCoffee(UUID coffeeId) {
			deletedCoffeeIds.add(coffeeId);
		}

		@Override
		public List<CoffeePhotoView> findAll() {
			return List.of();
		}

		@Override
		public long count() {
			return 0;
		}
	}

	private static class RecordingOpeningHoursProjectionRepository implements CoffeeOpeningHoursProjectionRepository {
		private final List<UUID> deletedCoffeeIds = new ArrayList<>();

		@Override
		public void insertSeed(CoffeeOpeningHoursView view) {
		}

		@Override
		public void replaceForCoffee(UUID coffeeId, List<CoffeeOpeningHoursView> openingHours) {
		}

		@Override
		public void deleteForCoffee(UUID coffeeId) {
			deletedCoffeeIds.add(coffeeId);
		}

		@Override
		public List<CoffeeOpeningHoursView> findAll() {
			return List.of();
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
