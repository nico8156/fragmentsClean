package com.nm.fragmentsclean.coffeeContext.read;

import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeePhotoProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeePhotoView;
import com.nm.fragmentsclean.platform.eventing.contracts.CoffeePhotosArrangedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;

public class CoffeePhotosArrangedEventHandler {
    private final CoffeePhotoProjectionRepository photos;
    private final CoffeePublicProjectionChangePolicy publicPolicy;
    private final ProjectionSyncPublisher sync;
    public CoffeePhotosArrangedEventHandler(CoffeePhotoProjectionRepository photos,
            CoffeeProjectionRepository coffees, ProjectionSyncPublisher sync) {
        this.photos=photos; this.publicPolicy=new CoffeePublicProjectionChangePolicy(coffees); this.sync=sync;
    }
    @Transactional public void handle(CoffeePhotosArrangedIntegrationEvent event) {
        photos.replaceForCoffee(event.coffeeId(), event.photos().stream().map(photo -> new CoffeePhotoView(
                photo.photoId(), event.coffeeId(), photo.photoUri(), photo.cover(), photo.sortOrder())).toList());
        if (!publicPolicy.isPubliclyVisible(event.coffeeId())) return;
        sync.publish(ProjectionSyncEvent.projectionUpdated("coffees", "entity", event.coffeeId().toString(),
                (long) event.version(), event.occurredAt(), List.of("photos")));
    }
}
