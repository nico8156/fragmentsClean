package com.nm.fragmentsclean.coffeeContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.GooglePlaceOpeningHoursGateway;
import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.repositories.fakes.FakeCoffeeRepository;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.Coffee;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeOpeningHoursImportedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.Address;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeName;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.GeoPoint;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.GooglePlaceId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.PhoneNumber;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.Tag;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.WebsiteUrl;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.ImportGoogleOpeningHoursForCoffee;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;

class ImportGoogleOpeningHoursForCoffeeTest {
	private static final Instant NOW = Instant.parse("2026-07-04T10:15:30Z");

	@Test
	void imports_google_opening_hours_after_coffee_created_and_publishes_domain_event() {
		var gateway = new RecordingOpeningHoursGateway(new GooglePlaceOpeningHoursGateway.ImportedOpeningHours(List.of(
				new GooglePlaceOpeningHoursGateway.OpeningPeriod(0, 480, 1080)), List.of("Monday: 8:00 AM - 6:00 PM")));
		var publisher = new RecordingDomainEventPublisher();
		var created = coffeeCreatedEvent(new GooglePlaceId("places/google-1"));
		var coffees = coffeesContaining(created);
		var useCase = new ImportGoogleOpeningHoursForCoffee(gateway, publisher, fixedClock(), coffees);

		useCase.handle(created);

		assertThat(gateway.requestedPlaceIds).containsExactly(created.googlePlaceId());
		assertThat(publisher.events).hasSize(1);
		var imported = (CoffeeOpeningHoursImportedEvent) publisher.events.getFirst();
		assertThat(imported.eventId()).isNotNull();
		assertThat(imported.commandId()).isEqualTo(created.commandId());
		assertThat(imported.coffeeId()).isEqualTo(created.coffeeId());
		assertThat(imported.googlePlaceId()).isEqualTo(created.googlePlaceId());
		assertThat(imported.periods()).containsExactly(new CoffeeOpeningHoursImportedEvent.OpeningPeriod(0, 480, 1080));
		assertThat(imported.weekdayDescriptions()).containsExactly("Monday: 8:00 AM - 6:00 PM");
		assertThat(imported.version()).isEqualTo(created.version() + 1);
		assertThat(coffees.findById(created.coffeeId()).orElseThrow().openingHours().windowsFor(com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.DayOfWeekShort.MONDAY)).hasSize(1);
		assertThat(imported.occurredAt()).isEqualTo(NOW);
		assertThat(imported.clientAt()).isEqualTo(created.clientAt());
	}

	@Test
	void ignores_coffees_without_google_place_id() {
		var gateway = new RecordingOpeningHoursGateway(new GooglePlaceOpeningHoursGateway.ImportedOpeningHours(List.of(), List.of("Monday: 8:00 AM - 6:00 PM")));
		var publisher = new RecordingDomainEventPublisher();
		var useCase = new ImportGoogleOpeningHoursForCoffee(gateway, publisher, fixedClock(), new FakeCoffeeRepository());

		useCase.handle(coffeeCreatedEvent(null));

		assertThat(gateway.requestedPlaceIds).isEmpty();
		assertThat(publisher.events).isEmpty();
	}

	private static DateTimeProvider fixedClock() {
		return () -> NOW;
	}

	private static CoffeeCreatedEvent coffeeCreatedEvent(GooglePlaceId googlePlaceId) {
		return new CoffeeCreatedEvent(
				UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
				UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
				new CoffeeId(UUID.fromString("11111111-1111-1111-1111-111111111111")),
				googlePlaceId,
				new CoffeeName("Fragments Cafe"),
				new Address("1 rue Example", "Rennes", "35000", "FR"),
				new GeoPoint(48.11, -1.67),
				new PhoneNumber("0200000000"),
				new WebsiteUrl("https://example.com"),
				List.of(new Tag("google-places")),
				12,
				Instant.parse("2026-07-04T10:00:00Z"),
				Instant.parse("2026-07-04T09:59:59Z"));
	}

	private static FakeCoffeeRepository coffeesContaining(CoffeeCreatedEvent event) {
		var result = new FakeCoffeeRepository();
		result.save(Coffee.createNew(event.coffeeId(), event.googlePlaceId(), event.name(), event.address(), event.location(),
				event.phoneNumber(), event.website(), java.util.Set.copyOf(event.tags()), event.occurredAt()));
		return result;
	}

	private static class RecordingOpeningHoursGateway implements GooglePlaceOpeningHoursGateway {
		private final ImportedOpeningHours openingHours;
		private final List<GooglePlaceId> requestedPlaceIds = new ArrayList<>();

		RecordingOpeningHoursGateway(ImportedOpeningHours openingHours) {
			this.openingHours = openingHours;
		}

		@Override
		public ImportedOpeningHours findOpeningHours(GooglePlaceId googlePlaceId) {
			requestedPlaceIds.add(googlePlaceId);
			return openingHours;
		}
	}

	private static class RecordingDomainEventPublisher implements DomainEventPublisher {
		private final List<DomainEvent> events = new ArrayList<>();

		@Override
		public void publish(DomainEvent event) {
			events.add(event);
		}
	}
}
