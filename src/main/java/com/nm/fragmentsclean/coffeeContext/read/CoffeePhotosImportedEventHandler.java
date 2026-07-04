package com.nm.fragmentsclean.coffeeContext.read;

import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeePhotoProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeePhotoView;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotosImportedEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public class CoffeePhotosImportedEventHandler implements EventHandler<CoffeePhotosImportedEvent> {
	private final CoffeePhotoProjectionRepository projectionRepository;
	private final ProjectionSyncPublisher projectionSyncPublisher;

	public CoffeePhotosImportedEventHandler(
			CoffeePhotoProjectionRepository projectionRepository,
			ProjectionSyncPublisher projectionSyncPublisher) {
		this.projectionRepository = projectionRepository;
		this.projectionSyncPublisher = projectionSyncPublisher;
	}

	@Override
	@Transactional
	public void handle(CoffeePhotosImportedEvent event) {
		var coffeeId = event.coffeeId().value();
		projectionRepository.replaceForCoffee(coffeeId, event.photos().stream()
				.map(photo -> new CoffeePhotoView(photo.photoId(), coffeeId, photo.photoUri()))
				.toList());
		projectionSyncPublisher.publish(ProjectionSyncEvent.projectionUpdated(
				"coffees",
				"entity",
				coffeeId.toString(),
				event.version(),
				event.occurredAt(),
				List.of("photos")));
	}
}
