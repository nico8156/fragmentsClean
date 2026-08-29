package com.nm.fragmentsclean.coffeeContext.read;

import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class CoffeeCreatedEventHandler implements EventHandler<CoffeeCreatedEvent> {

    private static final Logger log = LoggerFactory.getLogger(CoffeeCreatedEventHandler.class);

    private final CoffeeProjectionRepository projectionRepository;
    private final ProjectionSyncPublisher projectionSyncPublisher;

    public CoffeeCreatedEventHandler(
            CoffeeProjectionRepository projectionRepository,
            ProjectionSyncPublisher projectionSyncPublisher) {
        this.projectionRepository = projectionRepository;
        this.projectionSyncPublisher = projectionSyncPublisher;
    }


    @Override
    @Transactional
    public void handle(CoffeeCreatedEvent event) {
        log.info("Applying CoffeeCreatedEvent to projection for coffeeId={}", event.coffeeId().value());
		var mutation = projectionRepository.applyIfNewer(event);
		if (!mutation.applied()) {
			log.info("Ignoring stale CoffeeCreatedEvent for coffeeId={}, eventVersion={}, projectionVersion={}",
					event.coffeeId().value(), event.version(), mutation.version());
			return;
		}
        projectionSyncPublisher.publish(ProjectionSyncEvent.projectionUpdated(
                "coffees",
                "entity",
                event.coffeeId().value().toString(),
				mutation.version(),
				mutation.changedAt(),
                List.of("summary")));
    }
}
