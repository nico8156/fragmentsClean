package com.nm.fragmentsclean.coffeeContext.read;

import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeePhotoProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeePhotoView;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotosImportedEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.CoffeePhotosImportedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class CoffeePhotosImportedEventHandler implements EventHandler<CoffeePhotosImportedEvent> {
	private final CoffeePhotoProjectionRepository projectionRepository;
	private final ProjectionSyncPublisher projectionSyncPublisher;
	private final CoffeePublicProjectionChangePolicy publicChangePolicy;

	public CoffeePhotosImportedEventHandler(
			CoffeePhotoProjectionRepository projectionRepository,
			CoffeeProjectionRepository coffeeProjectionRepository,
			ProjectionSyncPublisher projectionSyncPublisher) {
		this.projectionRepository = projectionRepository;
		this.projectionSyncPublisher = projectionSyncPublisher;
		this.publicChangePolicy = new CoffeePublicProjectionChangePolicy(coffeeProjectionRepository);
	}

	@Override
	@Transactional
	public void handle(CoffeePhotosImportedEvent event) {
		var coffeeId = event.coffeeId().value();
		replace(coffeeId, event.photos().stream()
				.map(photo -> new CoffeePhotoView(photo.photoId(), coffeeId, photo.photoUri()))
				.toList(), event.version(), event.occurredAt());
	}

	public void handle(CoffeePhotosImportedIntegrationEvent event) {
		replace(event.coffeeId(), event.photos().stream()
				.map(photo -> new CoffeePhotoView(photo.photoId(), event.coffeeId(), photo.photoUri()))
				.toList(), event.version(), event.occurredAt());
	}

	private void replace(java.util.UUID coffeeId, java.util.List<CoffeePhotoView> photos, long version,
			java.time.Instant occurredAt) {
		projectionRepository.replaceForCoffee(coffeeId, photos);
		if (!publicChangePolicy.isPubliclyVisible(coffeeId)) return;
		projectionSyncPublisher.publish(ProjectionSyncEvent.projectionUpdated(
				"coffees",
				"entity",
				coffeeId.toString(),
				version,
				occurredAt,
				List.of("photos")));
	}
}
