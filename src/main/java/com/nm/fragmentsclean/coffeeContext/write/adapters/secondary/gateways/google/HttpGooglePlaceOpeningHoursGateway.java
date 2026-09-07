package com.nm.fragmentsclean.coffeeContext.write.adapters.secondary.gateways.google;

import com.nm.fragmentsclean.coffeeContext.write.businessLogic.gateways.GooglePlaceOpeningHoursGateway;
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
import java.util.ArrayList;
import java.util.Objects;

@Component
public class HttpGooglePlaceOpeningHoursGateway implements GooglePlaceOpeningHoursGateway {
	private static final String FIELD_MASK = "regularOpeningHours.weekdayDescriptions,regularOpeningHours.periods";

	private final RestTemplate restTemplate;
	private final String apiKey;
	private final String baseUrl;
	private final String languageCode;
	private final String regionCode;

	public HttpGooglePlaceOpeningHoursGateway(
			RestTemplate restTemplate,
			@Value("${google.places.api-key:}") String apiKey,
			@Value("${google.places.base-url:https://places.googleapis.com/v1}") String baseUrl,
			@Value("${google.places.language-code:fr}") String languageCode,
			@Value("${google.places.region-code:FR}") String regionCode) {
		this.restTemplate = restTemplate;
		this.apiKey = apiKey;
		this.baseUrl = baseUrl;
		this.languageCode = languageCode;
		this.regionCode = regionCode;
	}

	@Override
	public ImportedOpeningHours findOpeningHours(GooglePlaceId googlePlaceId) {
		requireApiKey();
		var url = UriComponentsBuilder
				.fromHttpUrl(baseUrl + "/places/" + googlePlaceId.value())
				.queryParam("languageCode", languageCode)
				.queryParam("regionCode", regionCode)
				.toUriString();

		var headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set("X-Goog-Api-Key", apiKey);
		headers.set("X-Goog-FieldMask", FIELD_MASK);

		try {
			var response = restTemplate.exchange(
					url,
					HttpMethod.GET,
					new HttpEntity<>(null, headers),
					PlaceOpeningHoursResponse.class);
			var body = response.getBody();
			if (body == null || body.regularOpeningHours() == null
					) {
				return new ImportedOpeningHours(List.of(), List.of());
			}
			var descriptions = body.regularOpeningHours().weekdayDescriptions() == null ? List.<String>of() : body.regularOpeningHours().weekdayDescriptions().stream()
					.map(this::blankToNull)
					.filter(Objects::nonNull)
					.toList();
			return new ImportedOpeningHours(toPeriods(body.regularOpeningHours().periods()), descriptions);
		} catch (HttpStatusCodeException e) {
			throw new GooglePlaceOpeningHoursGatewayException(
					"Google Places opening hours request failed: " + e.getStatusCode(),
					e);
		} catch (GooglePlaceOpeningHoursGatewayException e) {
			throw e;
		} catch (RuntimeException e) {
			throw new GooglePlaceOpeningHoursGatewayException("Google Places opening hours response is invalid", e);
		}
	}

	private void requireApiKey() {
		if (apiKey == null || apiKey.isBlank()) {
			throw new GooglePlaceOpeningHoursGatewayException("Google Places API key is missing");
		}
	}

	private String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private record PlaceOpeningHoursResponse(OpeningHours regularOpeningHours) {
	}

	private List<OpeningPeriod> toPeriods(List<Period> periods) {
		if (periods == null) return List.of();
		var result = new ArrayList<OpeningPeriod>();
		for (var period : periods) {
			if (period == null || period.open() == null || period.close() == null) continue;
			int start = toMinutes(period.open()); int end = toMinutes(period.close());
			int startDay = toMondayFirst(period.open().day()); int endDay = toMondayFirst(period.close().day());
			if (startDay == endDay && start < end) result.add(new OpeningPeriod(startDay, start, end));
			else { result.add(new OpeningPeriod(startDay, start, 1440)); result.add(new OpeningPeriod(endDay, 0, end)); }
		}
		return List.copyOf(result);
	}
	private int toMinutes(OpenClose point) { return point.hour() * 60 + point.minute(); }
	private int toMondayFirst(int googleDay) { return googleDay == 0 ? 6 : googleDay - 1; }

	private record OpeningHours(List<String> weekdayDescriptions, List<Period> periods) {
	}
	private record Period(OpenClose open, OpenClose close) { }
	private record OpenClose(int day, int hour, int minute) { }
}
