package com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.CoffeePhotoStorage;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.GooglePlacePhotosGateway;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotosImportedEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;

import java.util.UUID;

public class ImportGooglePhotosForCoffee implements EventHandler<CoffeeCreatedEvent> {
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
		if (event.googlePlaceId() == null) {
			return;
		}

		var importedPhotos = photosGateway.findPhotos(event.googlePlaceId()).stream()
				.map(photo -> photoStorage.store(event.coffeeId(), event.googlePlaceId(), photo))
				.toList();
		if (importedPhotos.isEmpty()) {
			return;
		}

		var now = dateTimeProvider.now();
		domainEventPublisher.publish(new CoffeePhotosImportedEvent(
				UUID.randomUUID(),
				event.commandId(),
				event.coffeeId(),
				event.googlePlaceId(),
				importedPhotos,
				event.version(),
				now,
				event.clientAt()
		));
	}
}
