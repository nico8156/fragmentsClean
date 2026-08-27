package com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.CoffeePhotoStorage;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.GooglePlacePhotosGateway;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotosImportedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.GooglePlaceId;
import com.nm.fragmentsclean.platform.eventing.contracts.CoffeeCreatedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImportGooglePhotosForCoffee implements EventHandler<CoffeeCreatedEvent> {
	private static final Logger log = LoggerFactory.getLogger(ImportGooglePhotosForCoffee.class);

	private final GooglePlacePhotosGateway photosGateway;
	private final CoffeePhotoStorage photoStorage;
	private final DomainEventPublisher domainEventPublisher;
	private final DateTimeProvider dateTimeProvider;

	public ImportGooglePhotosForCoffee(
			GooglePlacePhotosGateway photosGateway,
			CoffeePhotoStorage photoStorage,
			DomainEventPublisher domainEventPublisher,
			DateTimeProvider dateTimeProvider) {
		this.photosGateway = photosGateway;
		this.photoStorage = photoStorage;
		this.domainEventPublisher = domainEventPublisher;
		this.dateTimeProvider = dateTimeProvider;
	}

	@Override
	public void handle(CoffeeCreatedEvent event) {
		handle(event.coffeeId(), event.commandId(), event.googlePlaceId(), event.version(), event.occurredAt(), event.clientAt());
	}

	public void handle(CoffeeCreatedIntegrationEvent event) {
		handle(new CoffeeId(event.coffeeId()), event.commandId(),
				event.googlePlaceId() == null ? null : new GooglePlaceId(event.googlePlaceId()),
				event.version(), event.occurredAt(), event.occurredAt());
	}

	private void handle(CoffeeId coffeeId, UUID commandId, GooglePlaceId googlePlaceId, int version,
			java.time.Instant occurredAt, java.time.Instant clientAt) {
		if (googlePlaceId == null) {
			log.info("Skip Google photo import for coffeeId={} because googlePlaceId is missing", coffeeId.value());
			return;
		}

		var googlePhotos = photosGateway.findPhotos(googlePlaceId);
		if (googlePhotos.isEmpty()) {
			log.info("Google photo import found no photos for coffeeId={} googlePlaceId={}",
					coffeeId.value(), googlePlaceId.value());
			return;
		}

		var importedPhotos = googlePhotos.stream()
				.map(photo -> photoStorage.store(coffeeId, googlePlaceId, photo))
				.toList();
		if (importedPhotos.isEmpty()) {
			log.info("Google photo import stored no photos for coffeeId={} googlePlaceId={}", coffeeId.value(), googlePlaceId.value());
			return;
		}

		var now = dateTimeProvider.now();
		log.info("Publishing CoffeePhotosImportedEvent for coffeeId={} importedPhotos={}", coffeeId.value(), importedPhotos.size());
		domainEventPublisher.publish(new CoffeePhotosImportedEvent(
				UUID.randomUUID(),
				commandId,
				coffeeId,
				googlePlaceId,
				importedPhotos,
				version,
				now,
				clientAt
		));
	}
}
