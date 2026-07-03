package com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.google;

import java.util.List;

public final class GooglePlaceResponseModels {
	private GooglePlaceResponseModels() {
	}

	public record SearchTextResponse(List<Place> places) {
	}

	public record Place(
			String id,
			String name,
			DisplayName displayName,
			String formattedAddress,
			List<AddressComponent> addressComponents,
			Location location,
			String nationalPhoneNumber,
			String internationalPhoneNumber,
			String websiteUri,
			OpeningHours regularOpeningHours,
			List<Photo> photos
	) {
	}

	public record DisplayName(String text, String languageCode) {
	}

	public record Location(double latitude, double longitude) {
	}

	public record AddressComponent(String longText, String shortText, List<String> types) {
	}

	public record OpeningHours(List<String> weekdayDescriptions) {
	}

	public record Photo(String name, Integer widthPx, Integer heightPx, List<AuthorAttribution> authorAttributions) {
	}

	public record AuthorAttribution(String displayName, String uri, String photoUri) {
	}
}
