package com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.GooglePlaceOpeningHoursGateway;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeOpeningHoursImportedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.GooglePlaceId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.repositories.CoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.OpeningHours;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.DayOfWeekShort;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.TimeWindowMinutes;
import com.nm.fragmentsclean.platform.eventing.contracts.CoffeeCreatedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;

import java.util.UUID;
import java.util.EnumMap;
import java.util.List;

public class ImportGoogleOpeningHoursForCoffee implements EventHandler<CoffeeCreatedEvent> {
	private final GooglePlaceOpeningHoursGateway openingHoursGateway;
	private final DomainEventPublisher domainEventPublisher;
	private final DateTimeProvider dateTimeProvider;
	private final CoffeeRepository coffees;

	public ImportGoogleOpeningHoursForCoffee(
			GooglePlaceOpeningHoursGateway openingHoursGateway,
			DomainEventPublisher domainEventPublisher,
			DateTimeProvider dateTimeProvider, CoffeeRepository coffees) {
		this.openingHoursGateway = openingHoursGateway;
		this.domainEventPublisher = domainEventPublisher;
		this.dateTimeProvider = dateTimeProvider;
		this.coffees = coffees;
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

		var imported = openingHoursGateway.findOpeningHours(googlePlaceId);
		var now = dateTimeProvider.now();
		var coffee = coffees.findById(coffeeId).orElseThrow(() -> new IllegalStateException("Coffee missing during Google enrichment"));
		if (!imported.periods().isEmpty()) {
			var byDay = new EnumMap<DayOfWeekShort, List<TimeWindowMinutes>>(DayOfWeekShort.class);
			for (var period : imported.periods()) byDay.computeIfAbsent(DayOfWeekShort.fromCode(period.dayCode()), ignored -> new java.util.ArrayList<>())
					.add(new TimeWindowMinutes(period.startMinute(), period.endMinute()));
			coffee.setOpeningHours(new OpeningHours(byDay), now);
			coffees.save(coffee);
		}
		domainEventPublisher.publish(new CoffeeOpeningHoursImportedEvent(
				UUID.randomUUID(),
				commandId,
				coffeeId,
				googlePlaceId,
				imported.periods().stream().map(period -> new CoffeeOpeningHoursImportedEvent.OpeningPeriod(period.dayCode(), period.startMinute(), period.endMinute())).toList(),
				imported.weekdayDescriptions(),
				coffee.version(),
				now,
				clientAt
		));
	}
}
