package com.nm.fragmentsclean.adminImportContextTest.unit;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import com.nm.fragmentsclean.adminImportContext.adapters.primary.rest.AdminImportPlacesController;
import com.nm.fragmentsclean.adminImportContext.adapters.primary.rest.security.AdminSecurityConfiguration;
import com.nm.fragmentsclean.adminImportContext.adapters.primary.rest.security.AdminSecurityProperties;
import com.nm.fragmentsclean.adminImportContext.adapters.primary.rest.security.AdminTokenAuthenticationFilter;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.CoffeeCreationResult;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.GooglePlaceCoffeeImportStatus;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.GooglePlaceCoffeePreview;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.GooglePlaceSearchResult;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.GooglePlacesGateway;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.ImportGooglePlaceCoffee;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.PreviewGooglePlaceCoffee;
import com.nm.fragmentsclean.adminImportContext.businessLogic.usecases.SearchGooglePlacesForCoffee;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminTokenSecurityTest {
	@Test
	void admin_route_without_token_returns_401() throws Exception {
		mockMvc("admin-secret").perform(get("/api/admin/import/places").param("query", "cafe"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void admin_route_with_invalid_token_returns_401() throws Exception {
		mockMvc("admin-secret").perform(get("/api/admin/import/places")
						.param("query", "cafe")
						.header(HttpHeaders.AUTHORIZATION, "Bearer wrong"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void admin_route_with_valid_token_reaches_controller() throws Exception {
		mockMvc("admin-secret").perform(get("/api/admin/import/places")
						.param("query", "cafe")
						.header(HttpHeaders.AUTHORIZATION, "Bearer admin-secret"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].googlePlaceId").value("ChIJ-google-place"));
	}

	@Test
	void admin_route_with_missing_configured_token_stays_closed() throws Exception {
		mockMvc("").perform(get("/api/admin/import/places")
						.param("query", "cafe")
						.header(HttpHeaders.AUTHORIZATION, "Bearer anything"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void preflight_from_localhost_is_allowed() throws Exception {
		mockMvc("admin-secret").perform(options("/api/admin/import/places")
						.header(HttpHeaders.ORIGIN, "http://localhost:5173")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type,Accept"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,OPTIONS"));
	}

	@Test
	void preflight_from_127_0_0_1_is_allowed() throws Exception {
		mockMvc("admin-secret").perform(options("/api/admin/import/places")
						.header(HttpHeaders.ORIGIN, "http://127.0.0.1:5173")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type,Accept"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://127.0.0.1:5173"))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, "GET,POST,OPTIONS"));
	}

	private MockMvc mockMvc(String token) {
		var properties = new AdminSecurityProperties();
		properties.setToken(token);
		CorsConfigurationSource corsConfigurationSource =
				new AdminSecurityConfiguration().adminCorsConfigurationSource();

		return MockMvcBuilders.standaloneSetup(controller())
				.addFilters(new CorsFilter(corsConfigurationSource), new AdminTokenAuthenticationFilter(properties))
				.build();
	}

	private AdminImportPlacesController controller() {
		var gateway = new FakeGooglePlacesGateway();
		var preview = new PreviewGooglePlaceCoffee(gateway);
		return new AdminImportPlacesController(
				new SearchGooglePlacesForCoffee(gateway),
				preview,
				new ImportGooglePlaceCoffee(
						preview,
						command -> new CoffeeCreationResult(
								command.coffeeId(),
								command.googlePlaceId(),
								GooglePlaceCoffeeImportStatus.IMPORTED
						),
						() -> UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"),
						() -> Instant.parse("2026-07-03T09:00:00Z")
				)
		);
	}

	private static class FakeGooglePlacesGateway implements GooglePlacesGateway {
		@Override
		public List<GooglePlaceSearchResult> searchCoffeePlaces(String query) {
			return List.of(new GooglePlaceSearchResult(
					"ChIJ-google-place",
					"Cafe",
					"Rennes",
					48.111,
					-1.679
			));
		}

		@Override
		public Optional<GooglePlaceCoffeePreview> findCoffeePreview(String googlePlaceId) {
			return Optional.empty();
		}
	}
}
