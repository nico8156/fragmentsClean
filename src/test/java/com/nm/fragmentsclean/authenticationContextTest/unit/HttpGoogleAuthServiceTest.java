package com.nm.fragmentsclean.authenticationContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.google.GoogleOAuthProperties;
import com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.google.HttpGoogleAuthService;

class HttpGoogleAuthServiceTest {

	@Test
	void mobile_pkce_exchange_posts_code_verifier_and_no_client_secret() {
		var restTemplate = new RestTemplate();
		var server = MockRestServiceServer.createServer(restTemplate);
		var properties = new GoogleOAuthProperties();
		properties.setMobileIosClientId("ios-client.apps.googleusercontent.com");
		properties.setMobileIosRedirectUri("com.googleusercontent.apps.255942605258-jisbuvlprrs8pp2qb6ft3psa6hg650fe:/oauthredirect");
		properties.setTokenUri("https://oauth2.googleapis.test/token");
		properties.setUserInfoUri("https://openidconnect.googleapis.test/v1/userinfo");
		var service = new HttpGoogleAuthService(restTemplate, properties);

		server.expect(requestTo("https://oauth2.googleapis.test/token"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
				.andExpect(content().string(containsString("code=auth-code-123")))
				.andExpect(content().string(containsString("client_id=ios-client.apps.googleusercontent.com")))
				.andExpect(content().string(containsString("redirect_uri=com.googleusercontent.apps.255942605258-jisbuvlprrs8pp2qb6ft3psa6hg650fe%3A%2Foauthredirect")))
				.andExpect(content().string(containsString("code_verifier=verifier-123")))
				.andExpect(content().string(containsString("grant_type=authorization_code")))
				.andExpect(content().string(not(containsString("client_secret"))))
				.andRespond(withSuccess("""
						{
						  "access_token": "google-access-token",
						  "expires_in": 3600,
						  "token_type": "Bearer"
						}
						""", MediaType.APPLICATION_JSON));

		server.expect(requestTo("https://openidconnect.googleapis.test/v1/userinfo"))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer google-access-token"))
				.andRespond(withSuccess("""
						{
						  "sub": "google-sub-1",
						  "email": "user@example.com",
						  "email_verified": true,
						  "name": "User Test",
						  "picture": "https://example.com/avatar.png"
						}
						""", MediaType.APPLICATION_JSON));

		var user = service.exchangeMobileAuthorizationCodeForUser(
				"auth-code-123",
				"verifier-123",
				"com.googleusercontent.apps.255942605258-jisbuvlprrs8pp2qb6ft3psa6hg650fe:/oauthredirect");

		assertThat(user.sub()).isEqualTo("google-sub-1");
		assertThat(user.email()).isEqualTo("user@example.com");
		assertThat(user.name()).isEqualTo("User Test");
		assertThat(user.pictureUrl()).isEqualTo("https://example.com/avatar.png");
		server.verify();
	}
}
