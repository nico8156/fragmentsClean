package com.nm.fragmentsclean.coffeeContext.read;

import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeePublishedEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;
import java.util.List;

public class CoffeePublishedEventHandler implements EventHandler<CoffeePublishedEvent> {
    private final CoffeeProjectionRepository repository;
    private final ProjectionSyncPublisher syncPublisher;
    public CoffeePublishedEventHandler(CoffeeProjectionRepository repository, ProjectionSyncPublisher syncPublisher) {
        this.repository = repository; this.syncPublisher = syncPublisher;
    }
    @Override public void handle(CoffeePublishedEvent event) {
        repository.markPublished(event.coffeeId().value(), event.version(), event.occurredAt());
        syncPublisher.publish(ProjectionSyncEvent.projectionUpdated("coffees", "entity",
                event.coffeeId().value().toString(), (long) event.version(), event.occurredAt(), List.of("summary", "publicationStatus")));
    }
}
