package com.nm.fragmentsclean.coffeeContext.read.adapters.secondary.gateways.repositories.bootstrap;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record OldCoffeeSeedRow(
		UUID id,
		@JsonProperty("google_id") String googleId,
		@JsonProperty("display_name") String displayName,
		@JsonProperty("formatted_address") String formattedAddress,
		@JsonProperty("national_phone_number") String nationalPhoneNumber,
		@JsonProperty("website_uri") String websiteUri,
		double latitude,
		double longitude) {
}
