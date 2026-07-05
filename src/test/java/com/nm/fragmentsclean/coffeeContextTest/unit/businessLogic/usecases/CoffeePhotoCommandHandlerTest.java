package com.nm.fragmentsclean.coffeeContextTest.unit.businessLogic.usecases;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.fakes.FakeCoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.CoffeePhotoStorage;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotoAddedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotoDeletedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.GooglePlacePhoto;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.ImportedCoffeePhoto;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.GooglePlaceId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.AddCoffeePhotoCommand;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.AddCoffeePhotoCommandHandler;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.CreateCoffeeCommand;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.CreateCoffeeCommandHandler;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.DeleteCoffeePhotoCommand;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.DeleteCoffeePhotoCommandHandler;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventPublisher.FakeDomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;

class CoffeePhotoCommandHandlerTest {
	private static final UUID COFFEE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID COMMAND_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private static final UUID PHOTO_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

	private final FakeCoffeeRepository coffeeRepository = new FakeCoffeeRepository();
	private final FakeDomainEventPublisher eventPublisher = new FakeDomainEventPublisher();
	private final FixedDateTimeProvider dateTimeProvider = new FixedDateTimeProvider(Instant.parse("2026-07-05T10:00:00Z"));
	private final RecordingPhotoStorage photoStorage = new RecordingPhotoStorage();

	@BeforeEach
	void setUp() {
		new CreateCoffeeCommandHandler(coffeeRepository, new FakeDomainEventPublisher(), dateTimeProvider)
				.execute(new CreateCoffeeCommand(
						UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
						COFFEE_ID,
						"google-place-1",
						"Fragments Cafe",
						"1 rue du Test",
						"Rennes",
						"35000",
						"FR",
						48.11,
						-1.67,
						null,
						null,
						List.of("admin"),
						Instant.parse("2026-07-05T09:59:00Z")));
	}

	@Test
	void add_photo_stores_binary_photo_and_publishes_photo_added_event() {
		var handler = new AddCoffeePhotoCommandHandler(
				coffeeRepository,
				photoStorage,
				eventPublisher,
				dateTimeProvider);

		handler.execute(new AddCoffeePhotoCommand(
				COMMAND_ID,
				COFFEE_ID,
				"ideal.jpg",
				"image/jpeg",
				"jpeg-bytes".getBytes(),
				Instant.parse("2026-07-05T09:59:59Z")));

		assertThat(photoStorage.calls).hasSize(1);
		assertThat(photoStorage.calls.getFirst().coffeeId.value()).isEqualTo(COFFEE_ID);
		assertThat(photoStorage.calls.getFirst().googlePlaceId.value()).isEqualTo("google-place-1");
		assertThat(photoStorage.calls.getFirst().photo.contentType()).isEqualTo("image/jpeg");
		assertThat(photoStorage.calls.getFirst().photo.bytes()).isEqualTo("jpeg-bytes".getBytes());

		assertThat(eventPublisher.published).hasSize(1);
		var event = (CoffeePhotoAddedEvent) eventPublisher.published.getFirst();
		assertThat(event.commandId()).isEqualTo(COMMAND_ID);
		assertThat(event.coffeeId().value()).isEqualTo(COFFEE_ID);
		assertThat(event.photo().photoId()).isEqualTo(PHOTO_ID);
		assertThat(event.photo().photoUri()).isEqualTo("s3://bucket/photo.jpg");
		assertThat(event.occurredAt()).isEqualTo(dateTimeProvider.now());
	}

	@Test
	void delete_photo_publishes_photo_deleted_event() {
		var handler = new DeleteCoffeePhotoCommandHandler(
				coffeeRepository,
				eventPublisher,
				dateTimeProvider);

		handler.execute(new DeleteCoffeePhotoCommand(
				COMMAND_ID,
				COFFEE_ID,
				PHOTO_ID,
				Instant.parse("2026-07-05T09:59:59Z")));

		assertThat(eventPublisher.published).hasSize(1);
		var event = (CoffeePhotoDeletedEvent) eventPublisher.published.getFirst();
		assertThat(event.commandId()).isEqualTo(COMMAND_ID);
		assertThat(event.coffeeId().value()).isEqualTo(COFFEE_ID);
		assertThat(event.photoId().value()).isEqualTo(PHOTO_ID);
		assertThat(event.occurredAt()).isEqualTo(dateTimeProvider.now());
	}

	private record FixedDateTimeProvider(Instant now) implements DateTimeProvider {
		@Override
		public Instant now() {
			return now;
		}
	}

	private record StorageCall(CoffeeId coffeeId, GooglePlaceId googlePlaceId, GooglePlacePhoto photo) {
	}

	private static class RecordingPhotoStorage implements CoffeePhotoStorage {
		private final List<StorageCall> calls = new ArrayList<>();

		@Override
		public ImportedCoffeePhoto store(CoffeeId coffeeId, GooglePlaceId googlePlaceId, GooglePlacePhoto photo) {
			calls.add(new StorageCall(coffeeId, googlePlaceId, photo));
			return new ImportedCoffeePhoto(PHOTO_ID, "s3://bucket/photo.jpg");
		}
	}
}
