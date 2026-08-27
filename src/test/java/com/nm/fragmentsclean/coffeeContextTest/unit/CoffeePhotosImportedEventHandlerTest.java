package com.nm.fragmentsclean.coffeeContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.nm.fragmentsclean.coffeeContext.read.CoffeePhotosImportedEventHandler;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeePhotoProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeePhotoView;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotosImportedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.ImportedCoffeePhoto;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.GooglePlaceId;
import com.nm.fragmentsclean.platform.eventing.contracts.CoffeePhotosImportedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;

class CoffeePhotosImportedEventHandlerTest {

	@Test
	void replaces_photo_projection_and_publishes_projection_sync_event() {
		var repository = new RecordingPhotoProjectionRepository();
		var syncPublisher = new RecordingProjectionSyncPublisher();
		var handler = new CoffeePhotosImportedEventHandler(repository, syncPublisher);
		var event = photosImportedEvent(List.of(
				new ImportedCoffeePhoto(
						UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001"),
						"https://cdn.fragments.test/photo-1.jpg"),
				new ImportedCoffeePhoto(
						UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002"),
						"https://cdn.fragments.test/photo-2.jpg")));

		handler.handle(event);

		assertThat(repository.replacedCoffeeIds).containsExactly(event.coffeeId().value());
		assertThat(repository.replacedPhotos).containsExactly(
				new CoffeePhotoView(
						UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001"),
						event.coffeeId().value(),
						"https://cdn.fragments.test/photo-1.jpg"),
				new CoffeePhotoView(
						UUID.fromString("aaaaaaaa-0000-0000-0000-000000000002"),
						event.coffeeId().value(),
						"https://cdn.fragments.test/photo-2.jpg"));
		assertThat(syncPublisher.events).hasSize(1);
		ProjectionSyncEvent syncEvent = syncPublisher.events.getFirst();
		assertThat(syncEvent.eventName()).isEqualTo("projection.updated");
		assertThat(syncEvent.projection()).isEqualTo("coffees");
		assertThat(syncEvent.scope()).isEqualTo("entity");
		assertThat(syncEvent.entityId()).isEqualTo(event.coffeeId().value().toString());
		assertThat(syncEvent.version()).isEqualTo(event.version());
		assertThat(syncEvent.hints()).containsExactly("photos");
	}

	@Test
	void replaces_photo_projection_from_primitive_sqs_contract() {
		var repository = new RecordingPhotoProjectionRepository();
		var syncPublisher = new RecordingProjectionSyncPublisher();
		var handler = new CoffeePhotosImportedEventHandler(repository, syncPublisher);
		var coffeeId = UUID.fromString("11111111-1111-1111-1111-111111111111");
		var photoId = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");

		handler.handle(new CoffeePhotosImportedIntegrationEvent(
			UUID.randomUUID(), UUID.randomUUID(), coffeeId,
			List.of(new CoffeePhotosImportedIntegrationEvent.PhotoReference(photoId, "s3://bucket/photo.jpg")),
			12, Instant.parse("2026-07-04T10:20:00Z"), Instant.parse("2026-07-04T10:19:59Z")));

		assertThat(repository.replacedCoffeeIds).containsExactly(coffeeId);
		assertThat(repository.replacedPhotos).containsExactly(new CoffeePhotoView(photoId, coffeeId, "s3://bucket/photo.jpg"));
	}

	private static CoffeePhotosImportedEvent photosImportedEvent(List<ImportedCoffeePhoto> photos) {
		return new CoffeePhotosImportedEvent(
				UUID.fromString("99999999-9999-9999-9999-999999999999"),
				UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
				new CoffeeId(UUID.fromString("11111111-1111-1111-1111-111111111111")),
				new GooglePlaceId("places/google-1"),
				photos,
				12,
				Instant.parse("2026-07-04T10:20:00Z"),
				Instant.parse("2026-07-04T10:19:59Z"));
	}

	private static class RecordingPhotoProjectionRepository implements CoffeePhotoProjectionRepository {
		private final List<UUID> replacedCoffeeIds = new ArrayList<>();
		private final List<CoffeePhotoView> replacedPhotos = new ArrayList<>();

		@Override
		public void insertSeed(CoffeePhotoView view) {
			throw new UnsupportedOperationException("not used");
		}

		@Override
		public void replaceForCoffee(UUID coffeeId, List<CoffeePhotoView> photos) {
			replacedCoffeeIds.add(coffeeId);
			replacedPhotos.addAll(photos);
		}

		@Override
		public void append(CoffeePhotoView photo) {
			throw new UnsupportedOperationException("not used");
		}

		@Override
		public void deletePhoto(UUID coffeeId, UUID photoId) {
			throw new UnsupportedOperationException("not used");
		}

		@Override
		public void deleteForCoffee(UUID coffeeId) {
			throw new UnsupportedOperationException("not used");
		}

		@Override
		public List<CoffeePhotoView> findAll() {
			throw new UnsupportedOperationException("not used");
		}

		@Override
		public long count() {
			throw new UnsupportedOperationException("not used");
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
