package com.nm.fragmentsclean.adminImportContextTest.unit;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.nm.fragmentsclean.adminImportContext.businessLogic.models.GooglePlaceCoffeePreview;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.CoffeeCreationPort;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.GooglePlacesGateway;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.UuidGenerator;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.ImportGooglePlaceCoffee;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.PreviewGooglePlaceCoffee;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases.CreateCoffeeCommand;

import static org.assertj.core.api.Assertions.assertThat;

class ImportGooglePlaceCoffeeTest {
	private static final UUID COMMAND_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
	private static final UUID COFFEE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

	@Test
	void generates_ids_preserves_google_place_id_and_delegates_to_create_coffee_command() {
		var gateway = new FakeGooglePlacesGateway(new GooglePlaceCoffeePreview(
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
				null,
				null
		));
		var coffeeCreation = new CapturingCoffeeCreationPort();
		var now = Instant.parse("2026-07-03T09:00:00Z");

		var useCase = new ImportGooglePlaceCoffee(
				new PreviewGooglePlaceCoffee(gateway),
				coffeeCreation,
				new FixedUuidGenerator(COMMAND_ID, COFFEE_ID),
				() -> now
		);

		var result = useCase.execute("ChIJ-google-place");

		assertThat(result.commandId()).isEqualTo(COMMAND_ID);
		assertThat(result.coffeeId()).isEqualTo(COFFEE_ID);
		assertThat(result.googlePlaceId()).isEqualTo("ChIJ-google-place");

		CreateCoffeeCommand command = coffeeCreation.command;
		assertThat(command.commandId()).isEqualTo(COMMAND_ID);
		assertThat(command.coffeeId()).isEqualTo(COFFEE_ID);
		assertThat(command.googlePlaceId()).isEqualTo("ChIJ-google-place");
		assertThat(command.name()).isEqualTo("Café du Centre");
		assertThat(command.addressLine1()).isEqualTo("12 Rue de la Paix");
		assertThat(command.city()).isEqualTo("Rennes");
		assertThat(command.postalCode()).isEqualTo("35000");
		assertThat(command.country()).isEqualTo("FR");
		assertThat(command.lat()).isEqualTo(48.111);
		assertThat(command.lon()).isEqualTo(-1.679);
		assertThat(command.phoneNumber()).isEqualTo("02 99 00 00 00");
		assertThat(command.website()).isEqualTo("https://cafe.example");
		assertThat(command.tags()).containsExactly("google-places");
		assertThat(command.clientAt()).isEqualTo(now);
	}

	private record FakeGooglePlacesGateway(GooglePlaceCoffeePreview preview) implements GooglePlacesGateway {
		@Override
		public java.util.List<com.nm.fragmentsclean.adminImportContext.businessLogic.models.GooglePlaceSearchResult> searchCoffeePlaces(String query) {
			return java.util.List.of();
		}

		@Override
		public Optional<GooglePlaceCoffeePreview> findCoffeePreview(String googlePlaceId) {
			return Optional.of(preview);
		}
	}

	private static class CapturingCoffeeCreationPort implements CoffeeCreationPort {
		private CreateCoffeeCommand command;

		@Override
		public void createCoffee(CreateCoffeeCommand command) {
			this.command = command;
		}
	}

	private static class FixedUuidGenerator implements UuidGenerator {
		private final ArrayDeque<UUID> values;

		private FixedUuidGenerator(UUID... values) {
			this.values = new ArrayDeque<>(java.util.List.of(values));
		}

		@Override
		public UUID generate() {
			return values.removeFirst();
		}
	}
}
