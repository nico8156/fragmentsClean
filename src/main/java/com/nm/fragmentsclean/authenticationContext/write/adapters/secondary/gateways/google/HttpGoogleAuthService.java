package com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.google;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.GoogleAuthService;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Component
@Profile("prod")
public class HttpGoogleAuthService implements GoogleAuthService {

    private final RestTemplate restTemplate;
    private final GoogleOAuthProperties properties;

    public HttpGoogleAuthService(RestTemplate restTemplate,
                                 GoogleOAuthProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    public GoogleUserInfo exchangeCodeForUser(String authorizationCode) {

        // 1️⃣ Échanger le code contre des tokens
        GoogleTokenResponse tokenResponse = exchangeCodeForTokens(authorizationCode);

        if (tokenResponse == null || tokenResponse.accessToken == null) {
            throw new IllegalStateException("No access_token from Google");
        }

        // 2️⃣ Appeler /userinfo pour récupérer le profil
        GoogleUserInfoResponse userInfo = fetchUserInfo(tokenResponse.accessToken);

        if (userInfo == null || userInfo.sub == null) {
            throw new IllegalStateException("No userinfo from Google");
        }

        // 3️⃣ Adapter au modèle domaine
        return new GoogleUserInfo(
                userInfo.sub,
                userInfo.email,
                userInfo.emailVerified != null && userInfo.emailVerified,
                userInfo.name,
                userInfo.picture
        );
    }

    private GoogleTokenResponse exchangeCodeForTokens(String authorizationCode) {
        String url = properties.getTokenUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", authorizationCode);
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());
        form.add("redirect_uri", properties.getRedirectUri());
        form.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);

        ResponseEntity<GoogleTokenResponse> response =
                restTemplate.postForEntity(url, entity, GoogleTokenResponse.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException(
                    "Google token exchange failed: " + response.getStatusCode());
        }

        return response.getBody();
    }

    private GoogleUserInfoResponse fetchUserInfo(String accessToken) {
        String url = properties.getUserInfoUri();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<GoogleUserInfoResponse> response =
                restTemplate.exchange(url, HttpMethod.GET, entity, GoogleUserInfoResponse.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException(
                    "Google userinfo failed: " + response.getStatusCode());
        }

        return response.getBody();
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
