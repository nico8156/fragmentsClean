package com.nm.fragmentsclean.coffeeContext.read;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeProjectionSource;
import com.nm.fragmentsclean.platform.eventing.contracts.CoffeeDetailsEditedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;

public class CoffeeDetailsEditedIntegrationEventHandler {
    private final CoffeeProjectionSource source;
    private final CoffeeProjectionRepository projection;
    private final ProjectionSyncPublisher sync;

    public CoffeeDetailsEditedIntegrationEventHandler(CoffeeProjectionSource source,
            CoffeeProjectionRepository projection, ProjectionSyncPublisher sync) {
        this.source = source;
        this.projection = projection;
        this.sync = sync;
    }

    @Transactional
    public void handle(CoffeeDetailsEditedIntegrationEvent event) {
        var snapshot = source.findByCoffeeId(event.coffeeId())
                .orElseThrow(() -> new IllegalStateException("Coffee source is missing for " + event.coffeeId()));
        var mutation = projection.applyIfNewer(snapshot);
        if (!mutation.applied()) return;
        if (!"PUBLISHED".equals(snapshot.publicationStatus())) return;
        sync.publish(ProjectionSyncEvent.projectionUpdated("coffees", "entity", event.coffeeId().toString(),
                mutation.version(), mutation.changedAt(), List.of("summary")));
    }
}
