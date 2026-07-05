package com.nm.fragmentsclean.coffeeContext.read;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeePhotoProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotoDeletedEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;

public class CoffeePhotoDeletedEventHandler implements EventHandler<CoffeePhotoDeletedEvent> {
	private final CoffeePhotoProjectionRepository projectionRepository;
	private final ProjectionSyncPublisher projectionSyncPublisher;

	public CoffeePhotoDeletedEventHandler(
			CoffeePhotoProjectionRepository projectionRepository,
			ProjectionSyncPublisher projectionSyncPublisher) {
		this.projectionRepository = projectionRepository;
		this.projectionSyncPublisher = projectionSyncPublisher;
	}

	@Override
	@Transactional
	public void handle(CoffeePhotoDeletedEvent event) {
		var coffeeId = event.coffeeId().value();
		projectionRepository.deletePhoto(coffeeId, event.photoId().value());
		projectionSyncPublisher.publish(ProjectionSyncEvent.projectionUpdated(
				"coffees",
				"entity",
				coffeeId.toString(),
				(long) event.version(),
				event.occurredAt(),
				List.of("photos")));
	}
}
