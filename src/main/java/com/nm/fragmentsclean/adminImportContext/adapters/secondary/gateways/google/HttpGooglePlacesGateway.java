package com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.google;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.google.GooglePlaceResponseModels.Photo;
import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.google.GooglePlaceResponseModels.PhotoMedia;
import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.google.GooglePlaceResponseModels.Place;
import com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.google.GooglePlaceResponseModels.SearchTextResponse;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.GooglePlaceCoffeePreview;
import com.nm.fragmentsclean.adminImportContext.businessLogic.models.GooglePlaceSearchResult;
import com.nm.fragmentsclean.adminImportContext.businessLogic.ports.GooglePlacesGateway;

@Component
public class HttpGooglePlacesGateway implements GooglePlacesGateway {
	private static final String SEARCH_FIELD_MASK = String.join(",",
			"places.id",
			"places.displayName",
			"places.formattedAddress",
			"places.location"
	);
	private static final String PREVIEW_FIELD_MASK = String.join(",",
			"id",
			"displayName",
			"formattedAddress",
			"addressComponents",
			"location",
			"nationalPhoneNumber",
			"internationalPhoneNumber",
			"websiteUri",
			"regularOpeningHours",
			"photos"
	);

	private final RestTemplate restTemplate;
	private final GooglePlacesProperties properties;
	private final GooglePlaceMapper mapper = new GooglePlaceMapper();

	public HttpGooglePlacesGateway(RestTemplate restTemplate, GooglePlacesProperties properties) {
		this.restTemplate = restTemplate;
		this.properties = properties;
	}

	@Override
	public List<GooglePlaceSearchResult> searchCoffeePlaces(String query) {
		requireApiKey();
		var url = properties.getBaseUrl() + "/places:searchText";
		var body = Map.of(
				"textQuery", query,
				"includedType", "cafe",
				"strictTypeFiltering", false,
				"maxResultCount", properties.getMaxSearchResults(),
				"languageCode", properties.getLanguageCode(),
				"regionCode", properties.getRegionCode()
		);
		var response = exchange(url, HttpMethod.POST, body, SEARCH_FIELD_MASK, SearchTextResponse.class);
		if (response == null || response.places() == null) {
			return List.of();
		}
		return response.places().stream()
				.map(mapper::toSearchResult)
				.toList();
	}

	@Override
	public Optional<GooglePlaceCoffeePreview> findCoffeePreview(String googlePlaceId) {
		requireApiKey();
		var url = properties.getBaseUrl() + "/places/" + googlePlaceId
				+ "?languageCode=" + properties.getLanguageCode()
				+ "&regionCode=" + properties.getRegionCode();
		Place place = exchange(url, HttpMethod.GET, null, PREVIEW_FIELD_MASK, Place.class);
		return Optional.ofNullable(place)
				.map(value -> mapper.toPreview(value, this::temporaryPhotoUri));
	}

	private String temporaryPhotoUri(Photo photo) {
		if (photo.name() == null || photo.name().isBlank()) {
			return null;
		}

		var url = UriComponentsBuilder
				.fromHttpUrl(properties.getBaseUrl() + "/" + mediaResourceName(photo.name()))
				.queryParam("maxWidthPx", properties.getPhotoMaxWidthPx())
				.queryParam("skipHttpRedirect", true)
				.toUriString();
		PhotoMedia media = exchange(url, HttpMethod.GET, null, null, PhotoMedia.class);
		return media != null ? media.photoUri() : null;
	}

	private String mediaResourceName(String photoName) {
		return photoName.endsWith("/media") ? photoName : photoName + "/media";
	}

	private <T> T exchange(String url, HttpMethod method, Object body, String fieldMask, Class<T> responseType) {
		try {
			var headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.set("X-Goog-Api-Key", properties.getApiKey());
			if (fieldMask != null && !fieldMask.isBlank()) {
				headers.set("X-Goog-FieldMask", fieldMask);
			}
			var response = restTemplate.exchange(url, method, new HttpEntity<>(body, headers), responseType);
			if (!response.getStatusCode().is2xxSuccessful()) {
				throw new GooglePlacesGatewayException("Google Places request failed: " + response.getStatusCode());
			}
			return response.getBody();
		} catch (HttpStatusCodeException e) {
			throw new GooglePlacesGatewayException("Google Places request failed: " + e.getStatusCode(), e);
		} catch (GooglePlacesGatewayException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new GooglePlacesGatewayException("Google Places response is invalid", e);
		}
	}

	private void requireApiKey() {
		if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
			throw new GooglePlacesGatewayException("Google Places API key is missing");
		}
	}
}
