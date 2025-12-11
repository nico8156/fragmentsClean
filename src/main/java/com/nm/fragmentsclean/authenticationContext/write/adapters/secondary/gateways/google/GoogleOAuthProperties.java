package com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.google;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "google.oauth")
public class GoogleOAuthProperties {

    /**
     * Client ID du type "Web application" créé pour le backend
     */
    private String clientId;

    /**
     * Client secret associé
     */
    private String clientSecret;

    /**
     * redirect_uri utilisée pour l’échange du code.
     * Pour un serverAuthCode depuis mobile, Google accepte souvent "" (empty string),
     * mais ça doit matcher ce que tu as configuré côté console.
     */
    private String redirectUri = "";

    /**
     * Endpoint token Google.
     */
    private String tokenUri = "https://oauth2.googleapis.com/token";

    /**
     * Endpoint userinfo OpenID Connect.
     */
    private String userInfoUri = "https://openidconnect.googleapis.com/v1/userinfo";

    // getters / setters...

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }

    public String getRedirectUri() { return redirectUri; }
    public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }

    public String getTokenUri() { return tokenUri; }
    public void setTokenUri(String tokenUri) { this.tokenUri = tokenUri; }

    public String getUserInfoUri() { return userInfoUri; }
    public void setUserInfoUri(String userInfoUri) { this.userInfoUri = userInfoUri; }
}
