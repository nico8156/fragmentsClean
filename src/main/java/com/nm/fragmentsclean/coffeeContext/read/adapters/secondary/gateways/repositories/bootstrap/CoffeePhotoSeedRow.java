package com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.bootstrap;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record CoffeePhotoSeedRow(
		UUID id,
		@JsonProperty("coffee_id") UUID coffeeId,
		@JsonProperty("photo_uri") String photoUri) {
}
