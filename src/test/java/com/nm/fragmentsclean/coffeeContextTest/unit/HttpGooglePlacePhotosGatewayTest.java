package com.nm.fragmentsclean.coffeeContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.google.HttpGooglePlacePhotosGateway;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.GooglePlaceId;

class HttpGooglePlacePhotosGatewayTest {

	@Test
	void fetches_google_place_photo_names_then_downloads_media_with_field_masks() {
		var restTemplate = new RestTemplate();
		var server = MockRestServiceServer.createServer(restTemplate);
		var gateway = new HttpGooglePlacePhotosGateway(
				restTemplate,
				"test-api-key",
				"https://places.googleapis.com/v1",
				"fr",
				"FR",
				900,
				1);

		server.expect(requestTo("https://places.googleapis.com/v1/places/ChIJ-place?languageCode=fr&regionCode=FR"))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("X-Goog-Api-Key", "test-api-key"))
				.andExpect(header("X-Goog-FieldMask", "photos"))
				.andRespond(withSuccess("""
						{
						  "photos": [
						    { "name": "places/ChIJ-place/photos/photo-1" },
						    { "name": "places/ChIJ-place/photos/photo-2" }
						  ]
						}
						""", MediaType.APPLICATION_JSON));

		server.expect(requestTo("https://places.googleapis.com/v1/places/ChIJ-place/photos/photo-1/media?maxWidthPx=900&skipHttpRedirect=true"))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("X-Goog-Api-Key", "test-api-key"))
				.andRespond(withSuccess("""
						{
						  "name": "places/ChIJ-place/photos/photo-1/media",
						  "photoUri": "https://lh3.googleusercontent.com/photo-1"
						}
						""", MediaType.APPLICATION_JSON));

		server.expect(requestTo("https://lh3.googleusercontent.com/photo-1"))
				.andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess("jpeg-bytes".getBytes(), MediaType.IMAGE_JPEG));

		var photos = gateway.findPhotos(new GooglePlaceId("ChIJ-place"));

		assertThat(photos).hasSize(1);
		assertThat(photos.getFirst().sourceName()).isEqualTo("places/ChIJ-place/photos/photo-1");
		assertThat(photos.getFirst().contentType()).isEqualTo("image/jpeg");
		assertThat(photos.getFirst().bytes()).isEqualTo("jpeg-bytes".getBytes());
		server.verify();
	}

	@Test
	void caps_google_place_photo_import_at_fifteen() {
		var restTemplate = new RestTemplate();
		var server = MockRestServiceServer.createServer(restTemplate);
		var gateway = new HttpGooglePlacePhotosGateway(
				restTemplate,
				"test-api-key",
				"https://places.googleapis.com/v1",
				"fr",
				"FR",
				900,
				20);
		var photoNames = IntStream.rangeClosed(1, 16)
				.mapToObj(index -> "places/ChIJ-place/photos/photo-" + index)
				.toList();
		var responseBody = "{\"photos\":[" + photoNames.stream()
				.map(name -> "{\"name\":\"" + name + "\"}")
				.collect(Collectors.joining(",")) + "]}";

		server.expect(requestTo("https://places.googleapis.com/v1/places/ChIJ-place?languageCode=fr&regionCode=FR"))
				.andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

		photoNames.stream().limit(15).forEach(photoName -> {
			var photoIndex = photoName.substring(photoName.lastIndexOf('-') + 1);
			server.expect(requestTo("https://places.googleapis.com/v1/" + photoName + "/media?maxWidthPx=900&skipHttpRedirect=true"))
					.andRespond(withSuccess("""
							{
							  "name": "%s/media",
							  "photoUri": "https://lh3.googleusercontent.com/photo-%s"
							}
							""".formatted(photoName, photoIndex), MediaType.APPLICATION_JSON));
			server.expect(requestTo("https://lh3.googleusercontent.com/photo-" + photoIndex))
					.andRespond(withSuccess(("jpeg-bytes-" + photoIndex).getBytes(), MediaType.IMAGE_JPEG));
		});

		var photos = gateway.findPhotos(new GooglePlaceId("ChIJ-place"));

		assertThat(photos).hasSize(15);
		assertThat(photos.getLast().sourceName()).isEqualTo("places/ChIJ-place/photos/photo-15");
		server.verify();
	}
}
