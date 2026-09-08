package com.nm.fragmentsclean.coffeeContext.read;

import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeUnpublishedEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public class CoffeeUnpublishedEventHandler implements EventHandler<CoffeeUnpublishedEvent> {
    private final CoffeeProjectionRepository repository;
    private final ProjectionSyncPublisher syncPublisher;
    private final CoffeePublicProjectionChangePolicy publicChangePolicy;

    public CoffeeUnpublishedEventHandler(CoffeeProjectionRepository repository, ProjectionSyncPublisher syncPublisher) {
        this.repository = repository;
        this.syncPublisher = syncPublisher;
        this.publicChangePolicy = new CoffeePublicProjectionChangePolicy(repository);
    }

    @Override
    @Transactional
    public void handle(CoffeeUnpublishedEvent event) {
        var coffeeId = event.coffeeId().value();
        boolean wasPubliclyVisible = publicChangePolicy.isPubliclyVisible(coffeeId);
        var mutation = repository.markDraftIfNewer(coffeeId, event.version(), event.occurredAt());
        if (!mutation.applied() || !wasPubliclyVisible) return;
        syncPublisher.publish(ProjectionSyncEvent.projectionUpdated("coffees", "entity",
                coffeeId.toString(), mutation.version(), mutation.changedAt(), List.of("summary", "publicationStatus")));
    }
}
