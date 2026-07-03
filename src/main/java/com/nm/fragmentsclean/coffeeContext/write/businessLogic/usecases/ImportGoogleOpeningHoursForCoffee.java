package com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.GooglePlaceOpeningHoursGateway;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeOpeningHoursImportedEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;

import java.util.UUID;

public class ImportGoogleOpeningHoursForCoffee implements EventHandler<CoffeeCreatedEvent> {
	private final GooglePlaceOpeningHoursGateway openingHoursGateway;
	private final DomainEventPublisher domainEventPublisher;
	private final DateTimeProvider dateTimeProvider;

	public ImportGoogleOpeningHoursForCoffee(
			GooglePlaceOpeningHoursGateway openingHoursGateway,
			DomainEventPublisher domainEventPublisher,
			DateTimeProvider dateTimeProvider) {
		this.openingHoursGateway = openingHoursGateway;
		this.domainEventPublisher = domainEventPublisher;
		this.dateTimeProvider = dateTimeProvider;
	}

	@Override
	public void handle(CoffeeCreatedEvent event) {
		if (event.googlePlaceId() == null) {
			return;
		}

		var weekdayDescriptions = openingHoursGateway.findWeekdayDescriptions(event.googlePlaceId());
		var now = dateTimeProvider.now();
		domainEventPublisher.publish(new CoffeeOpeningHoursImportedEvent(
				UUID.randomUUID(),
				event.commandId(),
				event.coffeeId(),
				event.googlePlaceId(),
				weekdayDescriptions,
				event.version(),
				now,
				event.clientAt()
		));
	}
}
