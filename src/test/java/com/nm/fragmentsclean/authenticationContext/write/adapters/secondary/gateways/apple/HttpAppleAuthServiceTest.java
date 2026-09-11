package com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.apple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.DeterministicDateTimeProvider;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class HttpAppleAuthServiceTest {
  @Test
  void exchanges_the_authorization_code_and_revokes_the_provider_refresh_token() throws Exception {
    var http = new RestTemplate();
    var server = MockRestServiceServer.bindTo(http).build();
    var properties = properties();
    var jwt =
        Jwt.withTokenValue("apple-id-token")
            .header("alg", "ES256")
            .subject("apple-user")
            .claim("email", "relay@privaterelay.appleid.com")
            .claim("email_verified", true)
            .build();
    var service =
        new HttpAppleAuthService(
            http, properties, new DeterministicDateTimeProvider(), ignored -> jwt);

    server
        .expect(once(), requestTo(properties.getTokenUri()))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
        .andExpect(
            content().string(org.hamcrest.Matchers.containsString("code=authorization-code")))
        .andRespond(
            withSuccess(
                "{\"refresh_token\":\"provider-refresh\",\"id_token\":\"apple-id-token\"}",
                MediaType.APPLICATION_JSON));
    server
        .expect(once(), requestTo(properties.getRevokeUri()))
        .andExpect(method(HttpMethod.POST))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("token=provider-refresh")))
        .andRespond(withSuccess());

    var profile = service.authenticate("native-identity-token", "authorization-code", "Nicolas");

    assertThat(profile.sub()).isEqualTo("apple-user");
    assertThat(profile.emailVerified()).isTrue();
    assertThat(profile.providerRefreshToken()).isEqualTo("provider-refresh");

    service.revoke(profile.providerRefreshToken());
    server.verify();
  }

  private AppleOAuthProperties properties() throws Exception {
    var key =
        new ECKeyGenerator(Curve.P_256).algorithm(JWSAlgorithm.ES256).keyID("APPLE_KEY").generate();
    String privateKey =
        Base64.getMimeEncoder(64, "\n".getBytes())
            .encodeToString(key.toECPrivateKey().getEncoded());
    var properties = new AppleOAuthProperties();
    properties.setClientId("com.fragments.app");
    properties.setTeamId("TEAM_ID");
    properties.setKeyId("APPLE_KEY");
    properties.setPrivateKey(
        "-----BEGIN PRIVATE KEY-----\n" + privateKey + "\n-----END PRIVATE KEY-----");
    properties.setTokenUri("https://apple.test/auth/token");
    properties.setRevokeUri("https://apple.test/auth/revoke");
    return properties;
  }
}
