package com.nm.fragmentsclean.adminImportContext.adapters.secondary.gateways.google;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "google.places")
public class GooglePlacesProperties {
	private String apiKey;
	private String baseUrl = "https://places.googleapis.com/v1";
	private String languageCode = "fr";
	private String regionCode = "FR";
	private int maxSearchResults = 10;
	private int photoMaxWidthPx = 1200;

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public String getLanguageCode() {
		return languageCode;
	}

	public void setLanguageCode(String languageCode) {
		this.languageCode = languageCode;
	}

	public String getRegionCode() {
		return regionCode;
	}

	public void setRegionCode(String regionCode) {
		this.regionCode = regionCode;
	}

	public int getMaxSearchResults() {
		return maxSearchResults;
	}

	public void setMaxSearchResults(int maxSearchResults) {
		this.maxSearchResults = maxSearchResults;
	}

	public int getPhotoMaxWidthPx() {
		return photoMaxWidthPx;
	}

	public void setPhotoMaxWidthPx(int photoMaxWidthPx) {
		this.photoMaxWidthPx = photoMaxWidthPx;
	}
}
