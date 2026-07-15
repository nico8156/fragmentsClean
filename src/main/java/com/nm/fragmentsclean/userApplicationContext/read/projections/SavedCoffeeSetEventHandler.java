package com.nm.fragmentsclean.userApplicationContext.read.projections;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;
import com.nm.fragmentsclean.userApplicationContext.read.adapters.secondary.repositories.JdbcSavedCoffeeProjectionRepository;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.SavedCoffeeSetEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class SavedCoffeeSetEventHandler implements EventHandler<SavedCoffeeSetEvent> {
	private final JdbcSavedCoffeeProjectionRepository projectionRepository;
	private final ProjectionSyncPublisher projectionSyncPublisher;

	public SavedCoffeeSetEventHandler(
			JdbcSavedCoffeeProjectionRepository projectionRepository,
			ProjectionSyncPublisher projectionSyncPublisher) {
		this.projectionRepository = projectionRepository;
		this.projectionSyncPublisher = projectionSyncPublisher;
	}

	@Override
	@Transactional
	public void handle(SavedCoffeeSetEvent event) {
		projectionRepository.apply(event);
		projectionSyncPublisher.publish(ProjectionSyncEvent.projectionUpdated(
				"savedCoffees",
				"user",
				event.userId().toString(),
				event.version(),
				event.occurredAt(),
				List.of(event.active() ? "saved" : "removed")));
	}
}
