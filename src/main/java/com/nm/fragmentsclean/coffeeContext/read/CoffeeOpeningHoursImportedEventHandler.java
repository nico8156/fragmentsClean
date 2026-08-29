package com.nm.fragmentsclean.coffeeContext.read;

import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeOpeningHoursProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.CoffeeProjectionRepository;
import com.nm.fragmentsclean.coffeeContext.read.projections.CoffeeOpeningHoursView;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeOpeningHoursImportedEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.projectionSync.ProjectionSyncPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

public class CoffeeOpeningHoursImportedEventHandler implements EventHandler<CoffeeOpeningHoursImportedEvent> {
	private final CoffeeOpeningHoursProjectionRepository projectionRepository;
	private final ProjectionSyncPublisher projectionSyncPublisher;
	private final CoffeePublicProjectionChangePolicy publicChangePolicy;

	public CoffeeOpeningHoursImportedEventHandler(
			CoffeeOpeningHoursProjectionRepository projectionRepository,
			CoffeeProjectionRepository coffeeProjectionRepository,
			ProjectionSyncPublisher projectionSyncPublisher) {
		this.projectionRepository = projectionRepository;
		this.projectionSyncPublisher = projectionSyncPublisher;
		this.publicChangePolicy = new CoffeePublicProjectionChangePolicy(coffeeProjectionRepository);
	}

	@Override
	@Transactional
	public void handle(CoffeeOpeningHoursImportedEvent event) {
		var coffeeId = event.coffeeId().value();
		projectionRepository.replaceForCoffee(coffeeId, toViews(coffeeId, event.weekdayDescriptions()));
		if (!publicChangePolicy.isPubliclyVisible(coffeeId)) return;
		projectionSyncPublisher.publish(ProjectionSyncEvent.projectionUpdated(
				"coffees",
				"entity",
				coffeeId.toString(),
				event.version(),
				event.occurredAt(),
				List.of("openingHours")));
	}

	private List<CoffeeOpeningHoursView> toViews(UUID coffeeId, List<String> weekdayDescriptions) {
		return IntStream.range(0, weekdayDescriptions.size())
				.mapToObj(index -> new CoffeeOpeningHoursView(
						UUID.nameUUIDFromBytes((coffeeId + ":opening-hours:" + index).getBytes(java.nio.charset.StandardCharsets.UTF_8)),
						coffeeId,
						weekdayDescriptions.get(index)))
				.toList();
	}
}
