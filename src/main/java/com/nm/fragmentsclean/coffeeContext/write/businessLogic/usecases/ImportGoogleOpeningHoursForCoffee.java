package com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.GooglePlaceOpeningHoursGateway;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeOpeningHoursImportedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.GooglePlaceId;
import com.nm.fragmentsclean.platform.eventing.contracts.CoffeeCreatedIntegrationEvent;
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
		handle(event.coffeeId(), event.commandId(), event.googlePlaceId(), event.version(), event.occurredAt(), event.clientAt());
	}

	public void handle(CoffeeCreatedIntegrationEvent event) {
		handle(new CoffeeId(event.coffeeId()), event.commandId(),
				event.googlePlaceId() == null ? null : new GooglePlaceId(event.googlePlaceId()),
				event.version(), event.occurredAt(), event.occurredAt());
	}

	private void handle(CoffeeId coffeeId, UUID commandId, GooglePlaceId googlePlaceId, int version,
			java.time.Instant occurredAt, java.time.Instant clientAt) {
		if (googlePlaceId == null) {
			return;
		}

		var weekdayDescriptions = openingHoursGateway.findWeekdayDescriptions(googlePlaceId);
		var now = dateTimeProvider.now();
		domainEventPublisher.publish(new CoffeeOpeningHoursImportedEvent(
				UUID.randomUUID(),
				commandId,
				coffeeId,
				googlePlaceId,
				weekdayDescriptions,
				version,
				now,
				clientAt
		));
	}
}
