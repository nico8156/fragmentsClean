package com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.google;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.GooglePlacePhotosGateway;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.GooglePlacePhoto;
import com.nm.fragmentsclean.coffeeContext.write.businessLogic.models.VO.GooglePlaceId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Objects;

@Component
public class HttpGooglePlacePhotosGateway implements GooglePlacePhotosGateway {
	private static final String PLACE_FIELD_MASK = "photos";

	private final RestTemplate restTemplate;
	private final String apiKey;
	private final String baseUrl;
	private final String languageCode;
	private final String regionCode;
	private final int maxWidthPx;
	private final int importLimit;

	public HttpGooglePlacePhotosGateway(
			RestTemplate restTemplate,
			@Value("${google.places.api-key:}") String apiKey,
			@Value("${google.places.base-url:https://places.googleapis.com/v1}") String baseUrl,
			@Value("${google.places.language-code:fr}") String languageCode,
			@Value("${google.places.region-code:FR}") String regionCode,
			@Value("${google.places.photo-max-width-px:1200}") int maxWidthPx,
			@Value("${google.places.photo-import-limit:3}") int importLimit) {
		this.restTemplate = restTemplate;
		this.apiKey = apiKey;
		this.baseUrl = baseUrl;
		this.languageCode = languageCode;
		this.regionCode = regionCode;
		this.maxWidthPx = maxWidthPx;
		this.importLimit = importLimit;
	}

	@Override
	public List<GooglePlacePhoto> findPhotos(GooglePlaceId googlePlaceId) {
		requireApiKey();
		var place = fetchPlacePhotos(googlePlaceId);
		if (place == null || place.photos() == null) {
			return List.of();
		}
		return place.photos().stream()
				.map(Photo::name)
				.map(this::blankToNull)
				.filter(Objects::nonNull)
				.limit(Math.max(importLimit, 0))
				.map(this::downloadPhoto)
				.toList();
	}

	private PlacePhotosResponse fetchPlacePhotos(GooglePlaceId googlePlaceId) {
		var url = UriComponentsBuilder
				.fromHttpUrl(baseUrl + "/places/" + googlePlaceId.value())
				.queryParam("languageCode", languageCode)
				.queryParam("regionCode", regionCode)
				.toUriString();
		return exchange(url, PLACE_FIELD_MASK, PlacePhotosResponse.class);
	}

	private GooglePlacePhoto downloadPhoto(String photoName) {
		var url = UriComponentsBuilder
				.fromHttpUrl(baseUrl + "/" + mediaResourceName(photoName))
				.queryParam("maxWidthPx", maxWidthPx)
				.queryParam("skipHttpRedirect", true)
				.toUriString();
		var media = exchangeWithMetadata(url, null, PhotoMedia.class).body();
		if (media == null || media.photoUri() == null || media.photoUri().isBlank()) {
			throw new GooglePlacePhotosGatewayException("Google Places photo media response is missing photoUri");
		}
		var response = downloadPhotoBytes(media.photoUri());
		var body = response == null ? null : response.body();
		if (body == null || body.length == 0) {
			throw new GooglePlacePhotosGatewayException("Google Places photo media response is empty");
		}
		var contentType = response.contentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : response.contentType().toString();
		return new GooglePlacePhoto(photoName, contentType, body);
	}

	private String mediaResourceName(String photoName) {
		return photoName.endsWith("/media") ? photoName : photoName + "/media";
	}

	private GoogleResponse<byte[]> downloadPhotoBytes(String photoUri) {
		try {
			var response = restTemplate.exchange(
					photoUri,
					HttpMethod.GET,
					new HttpEntity<>(null, new HttpHeaders()),
					byte[].class);
			if (!response.getStatusCode().is2xxSuccessful()) {
				throw new GooglePlacePhotosGatewayException("Google Places photo bytes request failed: " + response.getStatusCode());
			}
			return new GoogleResponse<>(response.getBody(), response.getHeaders().getContentType());
		} catch (HttpStatusCodeException e) {
			throw new GooglePlacePhotosGatewayException("Google Places photo bytes request failed: " + e.getStatusCode(), e);
		} catch (GooglePlacePhotosGatewayException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new GooglePlacePhotosGatewayException("Google Places photo bytes response is invalid", e);
		}
	}

	private <T> T exchange(String url, String fieldMask, Class<T> responseType) {
		return exchangeWithMetadata(url, fieldMask, responseType).body();
	}

	private <T> GoogleResponse<T> exchangeWithMetadata(String url, String fieldMask, Class<T> responseType) {
		try {
			var headers = new HttpHeaders();
			headers.set("X-Goog-Api-Key", apiKey);
			if (fieldMask != null && !fieldMask.isBlank()) {
				headers.set("X-Goog-FieldMask", fieldMask);
				headers.setContentType(MediaType.APPLICATION_JSON);
			}
			var response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null, headers), responseType);
			if (!response.getStatusCode().is2xxSuccessful()) {
				throw new GooglePlacePhotosGatewayException("Google Places photos request failed: " + response.getStatusCode());
			}
			return new GoogleResponse<>(response.getBody(), response.getHeaders().getContentType());
		} catch (HttpStatusCodeException e) {
			throw new GooglePlacePhotosGatewayException("Google Places photos request failed: " + e.getStatusCode(), e);
		} catch (GooglePlacePhotosGatewayException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new GooglePlacePhotosGatewayException("Google Places photos response is invalid", e);
		}
	}

	private void requireApiKey() {
		if (apiKey == null || apiKey.isBlank()) {
			throw new GooglePlacePhotosGatewayException("Google Places API key is missing");
		}
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private record GoogleResponse<T>(T body, MediaType contentType) {
	}

	private record PlacePhotosResponse(List<Photo> photos) {
	}

	private record Photo(String name) {
	}

	private record PhotoMedia(String name, String photoUri) {
	}
}
