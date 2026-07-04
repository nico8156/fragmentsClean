package com.nm.fragmentsclean.coffeeContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.CoffeePhotoStorage;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.GooglePlacePhotosGateway;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotosImportedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.GooglePlacePhoto;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.ImportedCoffeePhoto;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.Address;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeName;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.GeoPoint;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.GooglePlaceId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.PhoneNumber;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.Tag;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.WebsiteUrl;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.ImportGooglePhotosForCoffee;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;

class ImportGooglePhotosForCoffeeTest {
	private static final Instant NOW = Instant.parse("2026-07-04T10:15:30Z");

	@Test
	void imports_google_photos_after_coffee_created_and_publishes_domain_event() {
		var googlePhoto = new GooglePlacePhoto("places/google-1/photos/photo-1", "image/jpeg", "bytes".getBytes());
		var gateway = new RecordingPhotosGateway(List.of(googlePhoto));
		var storage = new RecordingPhotoStorage(List.of(new ImportedCoffeePhoto(
				UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001"),
				"https://cdn.fragments.test/coffees/coffee-1/photo-1.jpg")));
		var publisher = new RecordingDomainEventPublisher();
		var useCase = new ImportGooglePhotosForCoffee(gateway, storage, publisher, fixedClock());
		var created = coffeeCreatedEvent(new GooglePlaceId("places/google-1"));

		useCase.handle(created);

		assertThat(gateway.requestedPlaceIds).containsExactly(created.googlePlaceId());
		assertThat(storage.storedPhotos).containsExactly(googlePhoto);
		assertThat(storage.coffeeIds).containsExactly(created.coffeeId());
		assertThat(publisher.events).hasSize(1);
		var imported = (CoffeePhotosImportedEvent) publisher.events.getFirst();
		assertThat(imported.eventId()).isNotNull();
		assertThat(imported.commandId()).isEqualTo(created.commandId());
		assertThat(imported.coffeeId()).isEqualTo(created.coffeeId());
		assertThat(imported.googlePlaceId()).isEqualTo(created.googlePlaceId());
		assertThat(imported.photos()).containsExactly(new ImportedCoffeePhoto(
				UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001"),
				"https://cdn.fragments.test/coffees/coffee-1/photo-1.jpg"));
		assertThat(imported.version()).isEqualTo(created.version());
		assertThat(imported.occurredAt()).isEqualTo(NOW);
		assertThat(imported.clientAt()).isEqualTo(created.clientAt());
	}

	@Test
	void ignores_coffees_without_google_place_id() {
		var gateway = new RecordingPhotosGateway(List.of(new GooglePlacePhoto("photo", "image/jpeg", "bytes".getBytes())));
		var storage = new RecordingPhotoStorage(List.of());
		var publisher = new RecordingDomainEventPublisher();
		var useCase = new ImportGooglePhotosForCoffee(gateway, storage, publisher, fixedClock());

		useCase.handle(coffeeCreatedEvent(null));

		assertThat(gateway.requestedPlaceIds).isEmpty();
		assertThat(storage.storedPhotos).isEmpty();
		assertThat(publisher.events).isEmpty();
	}

	@Test
	void does_not_publish_when_google_has_no_photos() {
		var gateway = new RecordingPhotosGateway(List.of());
		var storage = new RecordingPhotoStorage(List.of());
		var publisher = new RecordingDomainEventPublisher();
		var useCase = new ImportGooglePhotosForCoffee(gateway, storage, publisher, fixedClock());

		useCase.handle(coffeeCreatedEvent(new GooglePlaceId("places/google-1")));

		assertThat(gateway.requestedPlaceIds).hasSize(1);
		assertThat(storage.storedPhotos).isEmpty();
		assertThat(publisher.events).isEmpty();
	}

	private static DateTimeProvider fixedClock() {
		return () -> NOW;
	}

	private static CoffeeCreatedEvent coffeeCreatedEvent(GooglePlaceId googlePlaceId) {
		return new CoffeeCreatedEvent(
				UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
				UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
				new CoffeeId(UUID.fromString("11111111-1111-1111-1111-111111111111")),
				googlePlaceId,
				new CoffeeName("Fragments Cafe"),
				new Address("1 rue Example", "Rennes", "35000", "FR"),
				new GeoPoint(48.11, -1.67),
				new PhoneNumber("0200000000"),
				new WebsiteUrl("https://example.com"),
				List.of(new Tag("google-places")),
				12,
				Instant.parse("2026-07-04T10:00:00Z"),
				Instant.parse("2026-07-04T09:59:59Z"));
	}

	private static class RecordingPhotosGateway implements GooglePlacePhotosGateway {
		private final List<GooglePlacePhoto> photos;
		private final List<GooglePlaceId> requestedPlaceIds = new ArrayList<>();

		RecordingPhotosGateway(List<GooglePlacePhoto> photos) {
			this.photos = photos;
		}

		@Override
		public List<GooglePlacePhoto> findPhotos(GooglePlaceId googlePlaceId) {
			requestedPlaceIds.add(googlePlaceId);
			return photos;
		}
	}

	private static class RecordingPhotoStorage implements CoffeePhotoStorage {
		private final List<ImportedCoffeePhoto> stored;
		private final List<CoffeeId> coffeeIds = new ArrayList<>();
		private final List<GooglePlacePhoto> storedPhotos = new ArrayList<>();

		RecordingPhotoStorage(List<ImportedCoffeePhoto> stored) {
			this.stored = stored;
		}

		@Override
		public ImportedCoffeePhoto store(CoffeeId coffeeId, GooglePlaceId googlePlaceId, GooglePlacePhoto photo) {
			coffeeIds.add(coffeeId);
			storedPhotos.add(photo);
			return stored.get(storedPhotos.size() - 1);
		}
	}

	private static class RecordingDomainEventPublisher implements DomainEventPublisher {
		private final List<DomainEvent> events = new ArrayList<>();

		@Override
		public void publish(DomainEvent event) {
			events.add(event);
		}
	}
}
