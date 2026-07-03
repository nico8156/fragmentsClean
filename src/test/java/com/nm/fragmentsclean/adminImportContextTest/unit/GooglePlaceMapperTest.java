package com.nm.fragmentsclean.adminImportContextTest.unit;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.google.GooglePlaceMapper;
import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.google.GooglePlaceResponseModels.AddressComponent;
import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.google.GooglePlaceResponseModels.AuthorAttribution;
import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.google.GooglePlaceResponseModels.DisplayName;
import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.google.GooglePlaceResponseModels.Location;
import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.google.GooglePlaceResponseModels.OpeningHours;
import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.google.GooglePlaceResponseModels.Photo;
import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.google.GooglePlaceResponseModels.Place;

import static org.assertj.core.api.Assertions.assertThat;

class GooglePlaceMapperTest {
	private final GooglePlaceMapper mapper = new GooglePlaceMapper();

	@Test
	void maps_google_place_to_fragments_preview() {
		var place = new Place(
				"ChIJ-google-place",
				"places/ChIJ-google-place",
				new DisplayName("Café du Centre", "fr"),
				"12 Rue de la Paix, 35000 Rennes, France",
				List.of(
						new AddressComponent("12", "12", List.of("street_number")),
						new AddressComponent("Rue de la Paix", "Rue de la Paix", List.of("route")),
						new AddressComponent("Rennes", "Rennes", List.of("locality")),
						new AddressComponent("35000", "35000", List.of("postal_code")),
						new AddressComponent("France", "FR", List.of("country"))
				),
				new Location(48.111, -1.679),
				"02 99 00 00 00",
				"+33 2 99 00 00 00",
				"https://cafe.example",
				new OpeningHours(List.of("lundi: 08:00-18:00", "mardi: 08:00-18:00")),
				List.of(new Photo(
						"places/ChIJ-google-place/photos/photo-resource",
						1200,
						800,
						List.of(new AuthorAttribution("Google User", "https://maps.example", null))
				))
		);

		var preview = mapper.toPreview(place);

		assertThat(preview.googlePlaceId()).isEqualTo("ChIJ-google-place");
		assertThat(preview.name()).isEqualTo("Café du Centre");
		assertThat(preview.formattedAddress()).isEqualTo("12 Rue de la Paix, 35000 Rennes, France");
		assertThat(preview.addressLine1()).isEqualTo("12 Rue de la Paix");
		assertThat(preview.city()).isEqualTo("Rennes");
		assertThat(preview.postalCode()).isEqualTo("35000");
		assertThat(preview.country()).isEqualTo("FR");
		assertThat(preview.latitude()).isEqualTo(48.111);
		assertThat(preview.longitude()).isEqualTo(-1.679);
		assertThat(preview.phoneNumber()).isEqualTo("02 99 00 00 00");
		assertThat(preview.website()).isEqualTo("https://cafe.example");
		assertThat(preview.openingHours()).containsExactly("lundi: 08:00-18:00", "mardi: 08:00-18:00");
		assertThat(preview.photos()).hasSize(1);
		assertThat(preview.photos().getFirst().name()).isEqualTo("places/ChIJ-google-place/photos/photo-resource");
		assertThat(preview.photos().getFirst().temporaryPhotoUri()).isNull();
		assertThat(preview.photos().getFirst().authorAttributions()).containsExactly("Google User");
	}

	@Test
	void maps_temporary_photo_uri_when_resolved_by_adapter() {
		var place = new Place(
				"ChIJ-google-place",
				"places/ChIJ-google-place",
				new DisplayName("Café du Centre", "fr"),
				"12 Rue de la Paix, 35000 Rennes, France",
				List.of(),
				new Location(48.111, -1.679),
				null,
				null,
				null,
				null,
				List.of(new Photo(
						"places/ChIJ-google-place/photos/photo-resource",
						1200,
						800,
						List.of()
				))
		);

		var preview = mapper.toPreview(place, photo -> "https://temporary.googleusercontent.test/photo");

		assertThat(preview.photos().getFirst().temporaryPhotoUri())
				.isEqualTo("https://temporary.googleusercontent.test/photo");
	}
}
