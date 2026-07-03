package com.nm.fragmentsclean.adminImportContextTest.unit;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.google.GooglePlacesProperties;
import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.google.HttpGooglePlacesGateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpGooglePlacesGatewayTest {
	@Test
	void resolves_google_place_photo_media_uri_for_preview() {
		var restTemplate = new RestTemplate();
		var server = MockRestServiceServer.createServer(restTemplate);
		var properties = new GooglePlacesProperties();
		properties.setApiKey("test-api-key");
		properties.setBaseUrl("https://places.googleapis.com/v1");
		properties.setPhotoMaxWidthPx(900);
		var gateway = new HttpGooglePlacesGateway(restTemplate, properties);

		server.expect(requestTo("https://places.googleapis.com/v1/places/ChIJ-place?languageCode=fr&regionCode=FR"))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("X-Goog-Api-Key", "test-api-key"))
				.andExpect(header("X-Goog-FieldMask",
						"id,displayName,formattedAddress,addressComponents,location,nationalPhoneNumber,internationalPhoneNumber,websiteUri,regularOpeningHours,photos"))
				.andRespond(withSuccess("""
						{
						  "id": "ChIJ-place",
						  "displayName": { "text": "Cafe test", "languageCode": "fr" },
						  "formattedAddress": "1 rue Test, Rennes",
						  "location": { "latitude": 48.11, "longitude": -1.67 },
						  "photos": [
						    {
						      "name": "places/ChIJ-place/photos/photo-1",
						      "widthPx": 1200,
						      "heightPx": 800,
						      "authorAttributions": [{ "displayName": "Google User" }]
						    }
						  ]
						}
						""", MediaType.APPLICATION_JSON));

		server.expect(requestTo("https://places.googleapis.com/v1/places/ChIJ-place/photos/photo-1/media?maxWidthPx=900&skipHttpRedirect=true"))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("X-Goog-Api-Key", "test-api-key"))
				.andRespond(withSuccess("""
						{
						  "name": "places/ChIJ-place/photos/photo-1/media",
						  "photoUri": "https://temporary.googleusercontent.test/photo-1"
						}
						""", MediaType.APPLICATION_JSON));

		var preview = gateway.findCoffeePreview("ChIJ-place").orElseThrow();

		assertThat(preview.photos()).hasSize(1);
		assertThat(preview.photos().getFirst().name()).isEqualTo("places/ChIJ-place/photos/photo-1");
		assertThat(preview.photos().getFirst().temporaryPhotoUri())
				.isEqualTo("https://temporary.googleusercontent.test/photo-1");
		assertThat(preview.photos().getFirst().authorAttributions()).containsExactly("Google User");
		server.verify();
	}
}
