package com.nm.fragmentsclean.coffeeContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.nm.fragmentsclean.coffeeContext.read.CoffeePhotoDeletedEventHandler;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeePhotoProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeePhotoView;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotoDeletedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.PhotoId;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;

class CoffeePhotoDeletedEventHandlerTest {
	@Test
	void deletes_photo_projection_and_publishes_projection_sync_event() {
		var repository = new RecordingPhotoProjectionRepository();
		var syncPublisher = new RecordingProjectionSyncPublisher();
		var handler = new CoffeePhotoDeletedEventHandler(repository, syncPublisher);
		var coffeeId = UUID.fromString("11111111-1111-1111-1111-111111111111");
		var photoId = UUID.fromString("22222222-2222-2222-2222-222222222222");

		handler.handle(new CoffeePhotoDeletedEvent(
				UUID.fromString("99999999-9999-9999-9999-999999999999"),
				UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
				new CoffeeId(coffeeId),
				new PhotoId(photoId),
				14,
				Instant.parse("2026-07-05T10:00:00Z"),
				Instant.parse("2026-07-05T09:59:59Z")));

		assertThat(repository.deleted).containsExactly(new DeletedPhoto(coffeeId, photoId));
		assertThat(syncPublisher.events).hasSize(1);
		assertThat(syncPublisher.events.getFirst().eventName()).isEqualTo("projection.updated");
		assertThat(syncPublisher.events.getFirst().projection()).isEqualTo("coffees");
		assertThat(syncPublisher.events.getFirst().hints()).containsExactly("photos");
	}

	private record DeletedPhoto(UUID coffeeId, UUID photoId) {
	}

	private static class RecordingPhotoProjectionRepository implements CoffeePhotoProjectionRepository {
		private final List<DeletedPhoto> deleted = new ArrayList<>();

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
			deleted.add(new DeletedPhoto(coffeeId, photoId));
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
