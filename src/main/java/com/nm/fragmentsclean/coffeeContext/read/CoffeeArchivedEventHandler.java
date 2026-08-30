package com.nm.fragmentsclean.coffeeContext.read;

import java.util.List;

import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeOpeningHoursProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeePhotoProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeArchivedEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;
import org.springframework.transaction.annotation.Transactional;

public class CoffeeArchivedEventHandler implements EventHandler<CoffeeArchivedEvent> {

	private final CoffeeProjectionRepository projectionRepository;
	private final CoffeePhotoProjectionRepository photoProjectionRepository;
	private final CoffeeOpeningHoursProjectionRepository openingHoursProjectionRepository;
	private final ProjectionSyncPublisher projectionSyncPublisher;
	private final CoffeePublicProjectionChangePolicy publicChangePolicy;

	public CoffeeArchivedEventHandler(
			CoffeeProjectionRepository projectionRepository,
			CoffeePhotoProjectionRepository photoProjectionRepository,
			CoffeeOpeningHoursProjectionRepository openingHoursProjectionRepository,
			ProjectionSyncPublisher projectionSyncPublisher) {
		this.projectionRepository = projectionRepository;
		this.photoProjectionRepository = photoProjectionRepository;
		this.openingHoursProjectionRepository = openingHoursProjectionRepository;
		this.projectionSyncPublisher = projectionSyncPublisher;
		this.publicChangePolicy = new CoffeePublicProjectionChangePolicy(projectionRepository);
	}

	@Override
	@Transactional
	public void handle(CoffeeArchivedEvent event) {
		var coffeeId = event.coffeeId().value();
		boolean wasPubliclyVisible = publicChangePolicy.isPubliclyVisible(coffeeId);
		var mutation = projectionRepository.markArchivedIfNewer(coffeeId, event.version(), event.occurredAt());
		if (!mutation.applied() || !wasPubliclyVisible) return;
		projectionSyncPublisher.publish(ProjectionSyncEvent.projectionUpdated(
				"coffees",
				"entity",
				coffeeId.toString(),
				mutation.version(),
				mutation.changedAt(),
				List.of("archived", "summary")
		));
	}
}
