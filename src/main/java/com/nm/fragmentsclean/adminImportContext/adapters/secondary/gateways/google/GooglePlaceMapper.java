package com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.google;

import java.util.List;
import java.util.Objects;

import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.google.GooglePlaceResponseModels.AddressComponent;
import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.google.GooglePlaceResponseModels.AuthorAttribution;
import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.google.GooglePlaceResponseModels.Photo;
import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.google.GooglePlaceResponseModels.Place;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.GooglePlaceCoffeePreview;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.GooglePlacePhotoPreview;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.GooglePlaceSearchResult;

public class GooglePlaceMapper {
	public GooglePlaceSearchResult toSearchResult(Place place) {
		requireUsablePlace(place);
		return new GooglePlaceSearchResult(
				place.id(),
				displayName(place),
				place.formattedAddress(),
				place.location().latitude(),
				place.location().longitude()
		);
	}

	public GooglePlaceCoffeePreview toPreview(Place place) {
		requireUsablePlace(place);
		var address = addressParts(place);
		return new GooglePlaceCoffeePreview(
				place.id(),
				displayName(place),
				place.formattedAddress(),
				address.line1(),
				address.city(),
				address.postalCode(),
				address.country(),
				place.location().latitude(),
				place.location().longitude(),
				firstNonBlank(place.nationalPhoneNumber(), place.internationalPhoneNumber()),
				blankToNull(place.websiteUri()),
				openingHours(place),
				photos(place)
		);
	}

	private void requireUsablePlace(Place place) {
		if (place == null || place.id() == null || place.id().isBlank()) {
			throw new GooglePlacesGatewayException("Google Places response is missing place id");
		}
		if (displayName(place) == null || displayName(place).isBlank()) {
			throw new GooglePlacesGatewayException("Google Places response is missing displayName.text");
		}
		if (place.location() == null) {
			throw new GooglePlacesGatewayException("Google Places response is missing location");
		}
	}

	private String displayName(Place place) {
		return place.displayName() != null ? blankToNull(place.displayName().text()) : null;
	}

	private AddressParts addressParts(Place place) {
		var streetNumber = componentLong(place.addressComponents(), "street_number");
		var route = componentLong(place.addressComponents(), "route");
		var line1 = joinNonBlank(" ", streetNumber, route);
		if (line1 == null) {
			line1 = blankToNull(place.formattedAddress());
		}

		return new AddressParts(
				line1,
				firstNonBlank(
						componentLong(place.addressComponents(), "locality"),
						componentLong(place.addressComponents(), "postal_town"),
						componentLong(place.addressComponents(), "administrative_area_level_2")
				),
				componentLong(place.addressComponents(), "postal_code"),
				componentShort(place.addressComponents(), "country")
		);
	}

	private String componentLong(List<AddressComponent> components, String type) {
		return componentText(components, type, false);
	}

	private String componentShort(List<AddressComponent> components, String type) {
		return componentText(components, type, true);
	}

	private String componentText(List<AddressComponent> components, String type, boolean shortText) {
		if (components == null) {
			return null;
		}
		return components.stream()
				.filter(component -> component.types() != null && component.types().contains(type))
				.map(component -> shortText ? component.shortText() : component.longText())
				.map(this::blankToNull)
				.filter(Objects::nonNull)
				.findFirst()
				.orElse(null);
	}

	private List<String> openingHours(Place place) {
		if (place.regularOpeningHours() == null || place.regularOpeningHours().weekdayDescriptions() == null) {
			return List.of();
		}
		return place.regularOpeningHours().weekdayDescriptions().stream()
				.map(this::blankToNull)
				.filter(Objects::nonNull)
				.toList();
	}

	private List<GooglePlacePhotoPreview> photos(Place place) {
		if (place.photos() == null) {
			return List.of();
		}
		return place.photos().stream()
				.filter(photo -> photo.name() != null && !photo.name().isBlank())
				.map(this::photoPreview)
				.toList();
	}

	private GooglePlacePhotoPreview photoPreview(Photo photo) {
		return new GooglePlacePhotoPreview(
				photo.name(),
				photo.widthPx(),
				photo.heightPx(),
				photo.authorAttributions() == null
						? List.of()
						: photo.authorAttributions().stream()
								.map(AuthorAttribution::displayName)
								.map(this::blankToNull)
								.filter(Objects::nonNull)
								.toList()
		);
	}

	private String firstNonBlank(String... values) {
		for (String value : values) {
			String normalized = blankToNull(value);
			if (normalized != null) {
				return normalized;
			}
		}
		return null;
	}

	private String joinNonBlank(String separator, String... values) {
		var joined = String.join(separator,
				List.of(values).stream()
						.map(this::blankToNull)
						.filter(Objects::nonNull)
						.toList());
		return blankToNull(joined);
	}

	private String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	private record AddressParts(String line1, String city, String postalCode, String country) {
	}
}
