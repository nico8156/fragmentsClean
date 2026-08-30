package com.nm.fragmentsclean.coffeeContext.read;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeePhotoProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeePhotoView;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePhotoAddedEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.CoffeePhotoAddedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;

public class CoffeePhotoAddedEventHandler implements EventHandler<CoffeePhotoAddedEvent> {
	private final CoffeePhotoProjectionRepository projectionRepository;
	private final ProjectionSyncPublisher projectionSyncPublisher;
	private final CoffeePublicProjectionChangePolicy publicChangePolicy;

	public CoffeePhotoAddedEventHandler(
			CoffeePhotoProjectionRepository projectionRepository,
			CoffeeProjectionRepository coffeeProjectionRepository,
			ProjectionSyncPublisher projectionSyncPublisher) {
		this.projectionRepository = projectionRepository;
		this.projectionSyncPublisher = projectionSyncPublisher;
		this.publicChangePolicy = new CoffeePublicProjectionChangePolicy(coffeeProjectionRepository);
	}

	@Override
	@Transactional
	public void handle(CoffeePhotoAddedEvent event) {
		append(event.coffeeId().value(), event.photo().photoId(), event.photo().photoUri(), event.version(), event.occurredAt());
	}

	public void handle(CoffeePhotoAddedIntegrationEvent event) {
		append(event.coffeeId(), event.photoId(), event.photoUri(), event.version(), event.occurredAt());
	}

	private void append(java.util.UUID coffeeId, java.util.UUID photoId, String photoUri, long version,
			java.time.Instant occurredAt) {
		projectionRepository.append(new CoffeePhotoView(
				photoId,
				coffeeId,
				photoUri));
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
