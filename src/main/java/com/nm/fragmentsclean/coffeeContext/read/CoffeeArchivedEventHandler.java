package com.nm.fragmentsclean.coffeeContext.read;

import java.util.List;

import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeOpeningHoursProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeePhotoProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeArchivedEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;

public class CoffeeArchivedEventHandler implements EventHandler<CoffeeArchivedEvent> {

	private final CoffeeProjectionRepository projectionRepository;
	private final CoffeePhotoProjectionRepository photoProjectionRepository;
	private final CoffeeOpeningHoursProjectionRepository openingHoursProjectionRepository;
	private final ProjectionSyncPublisher projectionSyncPublisher;

	public CoffeeArchivedEventHandler(
			CoffeeProjectionRepository projectionRepository,
			CoffeePhotoProjectionRepository photoProjectionRepository,
			CoffeeOpeningHoursProjectionRepository openingHoursProjectionRepository,
			ProjectionSyncPublisher projectionSyncPublisher) {
		this.projectionRepository = projectionRepository;
		this.photoProjectionRepository = photoProjectionRepository;
		this.openingHoursProjectionRepository = openingHoursProjectionRepository;
		this.projectionSyncPublisher = projectionSyncPublisher;
	}

	@Override
	public void handle(CoffeeArchivedEvent event) {
		var coffeeId = event.coffeeId().value();
		photoProjectionRepository.deleteForCoffee(coffeeId);
		openingHoursProjectionRepository.deleteForCoffee(coffeeId);
		projectionRepository.deleteByCoffeeId(coffeeId);
		projectionSyncPublisher.publish(ProjectionSyncEvent.projectionUpdated(
				"coffees",
				"entity",
				coffeeId.toString(),
				(long) event.version(),
				event.occurredAt(),
				List.of("archived", "summary", "photos", "openingHours")
		));
	}
}
