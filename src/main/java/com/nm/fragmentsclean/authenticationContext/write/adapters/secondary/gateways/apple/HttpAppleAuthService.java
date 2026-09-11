package com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.apple;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.AppleAuthService;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
@Profile("!auth_test")
public final class HttpAppleAuthService implements AppleAuthService {
  private final RestTemplate http;
  private final AppleOAuthProperties properties;
  private final DateTimeProvider clock;
  private final JwtDecoder decoder;

  @Autowired
  public HttpAppleAuthService(
      RestTemplate http, AppleOAuthProperties properties, DateTimeProvider clock) {
    this(http, properties, clock, appleJwtDecoder(properties));
  }

  HttpAppleAuthService(
      RestTemplate http,
      AppleOAuthProperties properties,
      DateTimeProvider clock,
      JwtDecoder decoder) {
    this.http = http;
    this.properties = properties;
    this.clock = clock;
    this.decoder = decoder;
  }

  private static JwtDecoder appleJwtDecoder(AppleOAuthProperties properties) {
    var jwtDecoder = NimbusJwtDecoder.withJwkSetUri(properties.getJwkSetUri()).build();
    OAuth2TokenValidator<Jwt> issuer =
        JwtValidators.createDefaultWithIssuer(properties.getIssuer());
    OAuth2TokenValidator<Jwt> audience =
        jwt ->
            jwt.getAudience().contains(required(properties.getClientId(), "apple.oauth.client-id"))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "Invalid Apple audience", null));
    jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuer, audience));
    return jwtDecoder;
  }

  @Override
  public AppleUserInfo authenticate(
      String identityToken, String authorizationCode, String displayName) {
    var response = postToken(authorizationCode);
    String token = response.idToken != null ? response.idToken : identityToken;
    Jwt jwt = decoder.decode(token);
    String email = jwt.getClaimAsString("email");
    Object verified = jwt.getClaims().get("email_verified");
    boolean emailVerified =
        Boolean.TRUE.equals(verified) || "true".equalsIgnoreCase(String.valueOf(verified));
    if (response.refreshToken == null || response.refreshToken.isBlank())
      throw new IllegalStateException("Apple refresh_token is missing");
    return new AppleUserInfo(
        jwt.getSubject(), email, emailVerified, displayName, response.refreshToken);
  }

  @Override
  public void revoke(String providerRefreshToken) {
    var form = new LinkedMultiValueMap<String, String>();
    form.add("client_id", required(properties.getClientId(), "apple.oauth.client-id"));
    form.add("client_secret", clientSecret());
    form.add("token", providerRefreshToken);
    form.add("token_type_hint", "refresh_token");
    var response = http.postForEntity(properties.getRevokeUri(), formEntity(form), Void.class);
    if (!response.getStatusCode().is2xxSuccessful())
      throw new IllegalStateException("Apple token revocation failed");
  }

  private AppleTokenResponse postToken(String code) {
    var form = new LinkedMultiValueMap<String, String>();
    form.add("client_id", required(properties.getClientId(), "apple.oauth.client-id"));
    form.add("client_secret", clientSecret());
    form.add("code", code);
    form.add("grant_type", "authorization_code");
    var response =
        http.postForEntity(properties.getTokenUri(), formEntity(form), AppleTokenResponse.class);
    if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null)
      throw new IllegalStateException("Apple token exchange failed");
    return response.getBody();
  }

  private HttpEntity<LinkedMultiValueMap<String, String>> formEntity(
      LinkedMultiValueMap<String, String> form) {
    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    return new HttpEntity<>(form, headers);
  }

  private String clientSecret() {
    try {
      String pem =
          required(properties.getPrivateKey(), "apple.oauth.private-key").replace("\\n", "\n");
      ECPrivateKey key = parsePrivateKey(pem);
      var now = clock.now();
      var claims =
          new JWTClaimsSet.Builder()
              .issuer(required(properties.getTeamId(), "apple.oauth.team-id"))
              .subject(required(properties.getClientId(), "apple.oauth.client-id"))
              .audience(properties.getIssuer())
              .issueTime(Date.from(now))
              .expirationTime(Date.from(now.plusSeconds(300)))
              .build();
      var jwt =
          new SignedJWT(
              new JWSHeader.Builder(JWSAlgorithm.ES256)
                  .keyID(required(properties.getKeyId(), "apple.oauth.key-id"))
                  .build(),
              claims);
      jwt.sign(new ECDSASigner(key));
      return jwt.serialize();
    } catch (Exception e) {
      throw new IllegalStateException("Cannot create Apple client secret", e);
    }
  }

  private static ECPrivateKey parsePrivateKey(String pem) throws Exception {
    String encoded =
        pem.replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", "");
    var specification = new PKCS8EncodedKeySpec(Base64.getDecoder().decode(encoded));
    return (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(specification);
  }

  private static String required(String value, String property) {
    if (value == null || value.isBlank())
      throw new IllegalStateException(property + " is required");
    return value;
  }

  private static final class AppleTokenResponse {
    @JsonProperty("refresh_token")
    public String refreshToken;

    @JsonProperty("id_token")
    public String idToken;
  }
}
