package com.nm.fragmentsclean.coffeeContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.nm.fragmentsclean.coffeeContext.read.CoffeePhotoAddedEventHandler;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeePhotoProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeePhotoView;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotoAddedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.ImportedCoffeePhoto;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.coffeeContextTest.support.PublishedCoffeeProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeSummaryView;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.CoffeePhotoAddedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;

class CoffeePhotoAddedEventHandlerTest {
	@Test
	void appends_photo_projection_and_publishes_projection_sync_event() {
		var repository = new RecordingPhotoProjectionRepository();
		var syncPublisher = new RecordingProjectionSyncPublisher();
		var handler = new CoffeePhotoAddedEventHandler(repository, new PublishedCoffeeProjectionRepository(), syncPublisher);
		var coffeeId = UUID.fromString("11111111-1111-1111-1111-111111111111");
		var photoId = UUID.fromString("22222222-2222-2222-2222-222222222222");

		handler.handle(new CoffeePhotoAddedEvent(
				UUID.fromString("99999999-9999-9999-9999-999999999999"),
				UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
				new CoffeeId(coffeeId),
				new ImportedCoffeePhoto(photoId, "s3://bucket/key.jpg"),
				13,
				Instant.parse("2026-07-05T10:00:00Z"),
				Instant.parse("2026-07-05T09:59:59Z")));

		assertThat(repository.appended).containsExactly(new CoffeePhotoView(photoId, coffeeId, "s3://bucket/key.jpg"));
		assertThat(syncPublisher.events).hasSize(1);
		assertThat(syncPublisher.events.getFirst().eventName()).isEqualTo("projection.updated");
		assertThat(syncPublisher.events.getFirst().projection()).isEqualTo("coffees");
		assertThat(syncPublisher.events.getFirst().hints()).containsExactly("photos");
	}

	@Test
	void appends_photo_from_primitive_sqs_contract() {
		var repository = new RecordingPhotoProjectionRepository();
		var syncPublisher = new RecordingProjectionSyncPublisher();
		var handler = new CoffeePhotoAddedEventHandler(repository, new PublishedCoffeeProjectionRepository(), syncPublisher);
		var coffeeId = UUID.fromString("11111111-1111-1111-1111-111111111111");
		var photoId = UUID.fromString("22222222-2222-2222-2222-222222222222");

		handler.handle(new CoffeePhotoAddedIntegrationEvent(
			UUID.randomUUID(), UUID.randomUUID(), coffeeId, photoId, "s3://bucket/key.jpg", 13,
			Instant.parse("2026-07-05T10:00:00Z"), Instant.parse("2026-07-05T09:59:59Z")));

		assertThat(repository.appended).containsExactly(new CoffeePhotoView(photoId, coffeeId, "s3://bucket/key.jpg"));
	}

	@Test
	void appends_draft_photo_without_notifying_the_public_mobile_catalogue() {
		var repository = new RecordingPhotoProjectionRepository();
		var syncPublisher = new RecordingProjectionSyncPublisher();
		var handler = new CoffeePhotoAddedEventHandler(repository, new DraftCoffeeProjectionRepository(), syncPublisher);
		var coffeeId = UUID.fromString("11111111-1111-1111-1111-111111111111");
		var photoId = UUID.fromString("22222222-2222-2222-2222-222222222222");

		handler.handle(new CoffeePhotoAddedIntegrationEvent(
				UUID.randomUUID(), UUID.randomUUID(), coffeeId, photoId, "s3://bucket/key.jpg", 13,
				Instant.parse("2026-07-05T10:00:00Z"), Instant.parse("2026-07-05T09:59:59Z")));

		assertThat(repository.appended).hasSize(1);
		assertThat(syncPublisher.events).isEmpty();
	}

	private static class DraftCoffeeProjectionRepository implements CoffeeProjectionRepository {
		@Override public boolean isPublished(UUID coffeeId) { return false; }
		@Override public void apply(CoffeeCreatedEvent event) { }
		@Override public void deleteByCoffeeId(UUID coffeeId) { }
		@Override public List<CoffeeSummaryView> findAll() { return List.of(); }
		@Override public void insertSeed(CoffeeSummaryView view) { }
		@Override public long count() { return 0; }
	}

	private static class RecordingPhotoProjectionRepository implements CoffeePhotoProjectionRepository {
		private final List<CoffeePhotoView> appended = new ArrayList<>();

		@Override
		public void insertSeed(CoffeePhotoView view) {
		}

		@Override
		public void replaceForCoffee(UUID coffeeId, List<CoffeePhotoView> photos) {
		}

		@Override
		public void append(CoffeePhotoView photo) {
			appended.add(photo);
		}

		@Override
		public void deletePhoto(UUID coffeeId, UUID photoId) {
		}

		@Override
		public void deleteForCoffee(UUID coffeeId) {
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

	private static class RecordingProjectionSyncPublisher implements ProjectionSyncPublisher {
		private final List<ProjectionSyncEvent> events = new ArrayList<>();

		@Override
		public void publish(ProjectionSyncEvent event) {
			events.add(event);
		}
	}
}
