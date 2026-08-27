package com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.google;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.GoogleAuthService;

@Component
@Profile("!auth_test")
public class HttpGoogleAuthService implements GoogleAuthService {

	private final RestTemplate restTemplate;
	private final GoogleOAuthProperties properties;

	public HttpGoogleAuthService(RestTemplate restTemplate,
			GoogleOAuthProperties properties) {
		this.restTemplate = restTemplate;
		this.properties = properties;
	}

	@Override
	public GoogleUserInfo exchangeMobileAuthorizationCodeForUser(
			String authorizationCode,
			String codeVerifier,
			String redirectUri) {
		GoogleTokenResponse tokenResponse = exchangeCodeForTokens(
				authorizationCode,
				codeVerifier,
				redirectUri,
				properties.getMobileIosClientId(),
				properties.getMobileIosRedirectUri(),
				null,
				"google.oauth.mobile-ios-client-id",
				"google.oauth.mobile-ios-redirect-uri");
		return fetchGoogleUser(tokenResponse);
	}

	@Override
	public GoogleUserInfo exchangeStudioAuthorizationCodeForUser(
			String authorizationCode,
			String codeVerifier,
			String redirectUri) {
		GoogleTokenResponse tokenResponse = exchangeCodeForTokens(
				authorizationCode,
				codeVerifier,
				redirectUri,
				properties.getStudioClientId(),
				properties.getStudioRedirectUri(),
				properties.getStudioClientSecret(),
				"google.oauth.studio-client-id",
				"google.oauth.studio-redirect-uri");
		return fetchGoogleUser(tokenResponse);
	}

	private GoogleUserInfo fetchGoogleUser(GoogleTokenResponse tokenResponse) {
		if (tokenResponse == null || tokenResponse.accessToken == null) {
			throw new IllegalStateException("No access_token from Google");
		}

		GoogleUserInfoResponse userInfo = fetchUserInfo(tokenResponse.accessToken);

		if (userInfo == null || userInfo.sub == null) {
			throw new IllegalStateException("No userinfo from Google");
		}

		return new GoogleUserInfo(
				userInfo.sub,
				userInfo.email,
				userInfo.emailVerified != null && userInfo.emailVerified,
				userInfo.name,
				userInfo.picture);
	}

	private GoogleTokenResponse exchangeCodeForTokens(
			String authorizationCode,
			String codeVerifier,
			String redirectUri,
			String clientIdValue,
			String expectedRedirectValue,
			String clientSecretValue,
			String clientIdProperty,
			String redirectProperty) {
		String clientId = requireConfigured(clientIdValue, clientIdProperty);
		String expectedRedirectUri = requireConfigured(
				expectedRedirectValue,
				redirectProperty);
		if (!expectedRedirectUri.equals(redirectUri)) {
			throw new IllegalArgumentException("Invalid OAuth redirectUri");
		}
		String url = properties.getTokenUri();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("code", authorizationCode);
		form.add("client_id", clientId);
		form.add("redirect_uri", expectedRedirectUri);
		form.add("code_verifier", codeVerifier);
		form.add("grant_type", "authorization_code");
		if (clientSecretValue != null) {
			form.add("client_secret", requireConfigured(clientSecretValue, "google.oauth.studio-client-secret"));
		}
		HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);

		ResponseEntity<GoogleTokenResponse> response = restTemplate.postForEntity(url, entity,
				GoogleTokenResponse.class);

		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new IllegalStateException(
					"Google mobile token exchange failed: " + response.getStatusCode());
		}

		return response.getBody();
	}

	private GoogleUserInfoResponse fetchUserInfo(String accessToken) {
		String url = properties.getUserInfoUri();

		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(accessToken);

		HttpEntity<Void> entity = new HttpEntity<>(headers);

		ResponseEntity<GoogleUserInfoResponse> response = restTemplate.exchange(url, HttpMethod.GET, entity,
				GoogleUserInfoResponse.class);

		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new IllegalStateException(
					"Google userinfo failed: " + response.getStatusCode());
		}

		return response.getBody();
	}

	private static String requireConfigured(String value, String propertyName) {
		if (value == null || value.isBlank()) {
			throw new IllegalStateException(propertyName + " is required");
		}
		return value;
	}

	// DTOs internes pour la désérialisation JSON
	private static class GoogleTokenResponse {
		@JsonProperty("access_token")
		public String accessToken;
		@JsonProperty("expires_in")
		public Long expiresIn;
		@JsonProperty("refresh_token")
		public String refreshToken;
		@JsonProperty("id_token")
		public String idToken;
		public String scope;
		@JsonProperty("token_type")
		public String tokenType;
	}

	private static class GoogleUserInfoResponse {
		public String sub;
		public String email;
		@JsonProperty("email_verified")
		public Boolean emailVerified;
		public String name;
		@JsonProperty("picture")
		public String picture;
	}
}
