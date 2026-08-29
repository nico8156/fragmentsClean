package com.nm.fragmentsclean.coffeeContext.read;

import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePublishedEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public class CoffeePublishedEventHandler implements EventHandler<CoffeePublishedEvent> {
    private final CoffeeProjectionRepository repository;
    private final ProjectionSyncPublisher syncPublisher;
    public CoffeePublishedEventHandler(CoffeeProjectionRepository repository, ProjectionSyncPublisher syncPublisher) {
        this.repository = repository; this.syncPublisher = syncPublisher;
    }
	@Override
	@Transactional
	public void handle(CoffeePublishedEvent event) {
		var mutation = repository.markPublishedIfNewer(event.coffeeId().value(), event.version(), event.occurredAt());
		if (!mutation.applied()) return;
        syncPublisher.publish(ProjectionSyncEvent.projectionUpdated("coffees", "entity",
				event.coffeeId().value().toString(), mutation.version(), mutation.changedAt(), List.of("summary", "publicationStatus")));
    }
}
