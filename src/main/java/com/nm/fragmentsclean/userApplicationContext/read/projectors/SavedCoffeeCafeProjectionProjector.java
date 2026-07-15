package com.nm.fragmentsclean.userApplicationContext.read.projectors;

import com.nm.fragmentsclean.platform.eventing.contracts.CoffeeCreatedIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.CoffeeLifecycleIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;
import com.nm.fragmentsclean.userApplicationContext.read.adapters.secondary.repositories.JdbcSavedCoffeeProjectionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
public class SavedCoffeeCafeProjectionProjector {
	private final JdbcSavedCoffeeProjectionRepository projectionRepository;
	private final ProjectionSyncPublisher projectionSyncPublisher;

	public SavedCoffeeCafeProjectionProjector(
			JdbcSavedCoffeeProjectionRepository projectionRepository,
			ProjectionSyncPublisher projectionSyncPublisher) {
		this.projectionRepository = projectionRepository;
		this.projectionSyncPublisher = projectionSyncPublisher;
	}

	@Transactional
	public void upsert(CoffeeCreatedIntegrationEvent event) {
		projectionRepository.upsertCoffee(event);
		publishForUsers(event.coffeeId(), event.version(), event.occurredAt(), List.of("coffee"));
	}

	@Transactional
	public void archive(CoffeeLifecycleIntegrationEvent event) {
		projectionRepository.markCoffeeArchived(event.coffeeId(), event.version(), event.occurredAt());
		publishForUsers(event.coffeeId(), event.version(), event.occurredAt(), List.of("coffee", "archived"));
	}

	private void publishForUsers(UUID coffeeId, long version, java.time.Instant occurredAt, List<String> hints) {
		for (UUID userId : projectionRepository.activeUserIdsForCoffee(coffeeId)) {
			projectionSyncPublisher.publish(ProjectionSyncEvent.projectionUpdated(
					"savedCoffees",
					"user",
					userId.toString(),
					version,
					occurredAt,
					hints));
		}
	}
}
