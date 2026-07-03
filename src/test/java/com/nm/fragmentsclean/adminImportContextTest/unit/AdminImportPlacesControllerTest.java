package com.nm.fragmentsclean.adminImportContextTest.unit;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.adminImportContext.adapters.primary.rest.AdminImportPlacesController;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.CoffeeCreationResult;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.GooglePlaceCoffeePreview;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.GooglePlaceCoffeeImportStatus;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.GooglePlaceSearchResult;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.GooglePlacesGateway;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.UuidGenerator;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.CreateCoffeeCommand;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.ImportGooglePlaceCoffee;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.PreviewGooglePlaceCoffee;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.SearchGooglePlacesForCoffee;

import static org.assertj.core.api.Assertions.assertThat;

class AdminImportPlacesControllerTest {
	@Test
	void exposes_search_preview_and_import_responses() throws Exception {
		var gateway = new FakeGooglePlacesGateway();
		var now = Instant.parse("2026-07-03T09:00:00Z");

		var controller = new AdminImportPlacesController(
				new SearchGooglePlacesForCoffee(gateway),
				new PreviewGooglePlaceCoffee(gateway),
				new ImportGooglePlaceCoffee(
						new PreviewGooglePlaceCoffee(gateway),
						new StatusCoffeeCreationPort(GooglePlaceCoffeeImportStatus.IMPORTED),
						new FixedUuidGenerator(),
						() -> now
				)
		);

		var search = controller.search("cafe rennes");
		assertThat(search).hasSize(1);
		assertThat(search.getFirst().googlePlaceId()).isEqualTo("ChIJ-google-place");

		var preview = controller.preview("ChIJ-google-place");
		assertThat(preview.googlePlaceId()).isEqualTo("ChIJ-google-place");
		assertThat(preview.info().name()).isEqualTo("Café du Centre");
		assertThat(preview.info().phoneNumber()).isEqualTo("02 99 00 00 00");
		assertThat(preview.openingHours().weekdayDescriptions()).containsExactly("lundi: 08:00-18:00");
		assertThat(preview.openingHours().periods()).isEmpty();
		var previewJson = new ObjectMapper().writeValueAsString(preview);
		assertThat(previewJson).contains("\"info\"");
		assertThat(previewJson).contains("\"openingHours\"");
		assertThat(previewJson).contains("\"weekdayDescriptions\"");
		assertThat(previewJson).doesNotContain("\"addressLine1\"");

		var imported = controller.importPlace("ChIJ-google-place");
		assertThat(imported.getStatusCode().value()).isEqualTo(202);
		assertThat(imported.getBody()).isNotNull();
		assertThat(imported.getBody().coffeeId()).isEqualTo(UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"));
		assertThat(imported.getBody().googlePlaceId()).isEqualTo("ChIJ-google-place");
		assertThat(imported.getBody().status()).isEqualTo("IMPORTED");
	}

	@Test
	void import_response_exposes_already_imported_status() {
		var gateway = new FakeGooglePlacesGateway();
		var controller = new AdminImportPlacesController(
				new SearchGooglePlacesForCoffee(gateway),
				new PreviewGooglePlaceCoffee(gateway),
				new ImportGooglePlaceCoffee(
						new PreviewGooglePlaceCoffee(gateway),
						new StatusCoffeeCreationPort(GooglePlaceCoffeeImportStatus.ALREADY_IMPORTED),
						new FixedUuidGenerator(),
						() -> Instant.parse("2026-07-03T09:00:00Z")
				)
		);

		var imported = controller.importPlace("ChIJ-google-place");

		assertThat(imported.getStatusCode().value()).isEqualTo(202);
		assertThat(imported.getBody()).isNotNull();
		assertThat(imported.getBody().coffeeId()).isNull();
		assertThat(imported.getBody().googlePlaceId()).isEqualTo("ChIJ-google-place");
		assertThat(imported.getBody().status()).isEqualTo("ALREADY_IMPORTED");
	}

	private static class FakeGooglePlacesGateway implements GooglePlacesGateway {
		@Override
		public List<GooglePlaceSearchResult> searchCoffeePlaces(String query) {
			return List.of(new GooglePlaceSearchResult(
					"ChIJ-google-place",
					"Café du Centre",
					"12 Rue de la Paix, 35000 Rennes, France",
					48.111,
					-1.679
			));
		}

		@Override
		public Optional<GooglePlaceCoffeePreview> findCoffeePreview(String googlePlaceId) {
			return Optional.of(new GooglePlaceCoffeePreview(
					"ChIJ-google-place",
					"Café du Centre",
					"12 Rue de la Paix, 35000 Rennes, France",
					"12 Rue de la Paix",
					"Rennes",
					"35000",
					"FR",
					48.111,
					-1.679,
					"02 99 00 00 00",
					"https://cafe.example",
					List.of("lundi: 08:00-18:00"),
					List.of()
			));
		}
	}

	private static class FixedUuidGenerator implements UuidGenerator {
		private int index;
		private final UUID[] values = {
				UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
				UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
		};

		@Override
		public UUID generate() {
			return values[index++];
		}
	}

	private record StatusCoffeeCreationPort(GooglePlaceCoffeeImportStatus status)
			implements com.nm.fragmentsclean.adminImportContext.businessLogic.ports.CoffeeCreationPort {
		@Override
		public CoffeeCreationResult createCoffee(CreateCoffeeCommand command) {
			return new CoffeeCreationResult(
					status == GooglePlaceCoffeeImportStatus.IMPORTED ? command.coffeeId() : null,
					command.googlePlaceId(),
					status
			);
		}
	}
}
