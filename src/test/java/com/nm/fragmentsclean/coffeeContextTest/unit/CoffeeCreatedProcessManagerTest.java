package com.nm.fragmentsclean.coffeeContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.nm.fragmentsclean.coffeeContext.businessLogic.processManagers.CoffeeCreatedProcessManager;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.CoffeeCreatedEvent;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.Address;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.CoffeeName;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.GeoPoint;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.GooglePlaceId;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.PhoneNumber;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.Tag;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.WebsiteUrl;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class CoffeeCreatedProcessManagerTest {

	@Test
	void runs_projection_then_enrichment_policies_when_coffee_is_created() {
		var calls = new ArrayList<String>();
		var event = coffeeCreatedEvent();
		var processManager = new CoffeeCreatedProcessManager(List.of(
				recordingHandler("summaryProjection", calls),
				recordingHandler("openingHoursImport", calls),
				recordingHandler("photosImport", calls)));

		processManager.handle(event);

		assertThat(calls).containsExactly(
				"summaryProjection:" + event.coffeeId().value(),
				"openingHoursImport:" + event.coffeeId().value(),
				"photosImport:" + event.coffeeId().value());
	}

	private static EventHandler<CoffeeCreatedEvent> recordingHandler(String name, List<String> calls) {
		return event -> calls.add(name + ":" + event.coffeeId().value());
	}

	private static CoffeeCreatedEvent coffeeCreatedEvent() {
		return new CoffeeCreatedEvent(
				UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
				UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
				new CoffeeId(UUID.fromString("11111111-1111-1111-1111-111111111111")),
				new GooglePlaceId("places/google-1"),
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
}
