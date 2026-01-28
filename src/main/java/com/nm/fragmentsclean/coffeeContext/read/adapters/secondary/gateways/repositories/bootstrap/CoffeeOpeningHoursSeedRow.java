package com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.bootstrap;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record CoffeeOpeningHoursSeedRow(
		UUID id,
		@JsonProperty("coffee_id") UUID coffeeId,
		@JsonProperty("weekday_description") String weekdayDescription) {
}
