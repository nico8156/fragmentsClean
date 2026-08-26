package com.nm.fragmentsclean.coffeeContext.read;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeProjectionSource;
import com.nm.fragmentsclean.platform.eventing.contracts.CoffeeCreatedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;

/** Applies the primitive coffee.created integration contract to the local read model. */
public class CoffeeCreatedIntegrationEventHandler {
	private final CoffeeProjectionSource projectionSource;
	private final CoffeeProjectionRepository projectionRepository;
	private final ProjectionSyncPublisher projectionSyncPublisher;

	public CoffeeCreatedIntegrationEventHandler(CoffeeProjectionSource projectionSource,
			CoffeeProjectionRepository projectionRepository, ProjectionSyncPublisher projectionSyncPublisher) {
		this.projectionSource = projectionSource;
		this.projectionRepository = projectionRepository;
		this.projectionSyncPublisher = projectionSyncPublisher;
	}

	@Transactional
	public void handle(CoffeeCreatedIntegrationEvent event) {
		var view = projectionSource.findByCoffeeId(event.coffeeId())
				.orElseThrow(() -> new IllegalStateException("Coffee source is missing for " + event.coffeeId()));
		projectionRepository.apply(view);
		projectionSyncPublisher.publish(ProjectionSyncEvent.projectionUpdated(
				"coffees", "entity", view.id().toString(), view.version(), view.updatedAt(), List.of("summary")));
	}
}
