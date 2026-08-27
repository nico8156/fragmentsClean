package com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.google;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "google.oauth")
public class GoogleOAuthProperties {

    /**
     * Client ID public iOS utilisé par le flow mobile Authorization Code + PKCE.
     */
    private String mobileIosClientId;

    /**
     * Redirect URI mobile iOS autorisée. La requête mobile doit matcher exactement cette valeur.
     */
    private String mobileIosRedirectUri;

    /** Client ID and redirect URI dedicated to the browser-based Studio PKCE flow. */
    private String studioClientId;
    private String studioRedirectUri;
    private String studioClientSecret;

    /**
     * Endpoint token Google.
     */
    private String tokenUri = "https://oauth2.googleapis.com/token";

    /**
     * Endpoint userinfo OpenID Connect.
     */
    private String userInfoUri = "https://openidconnect.googleapis.com/v1/userinfo";

    // getters / setters...

    public String getMobileIosClientId() { return mobileIosClientId; }
    public void setMobileIosClientId(String mobileIosClientId) { this.mobileIosClientId = mobileIosClientId; }

    public String getMobileIosRedirectUri() { return mobileIosRedirectUri; }
    public void setMobileIosRedirectUri(String mobileIosRedirectUri) { this.mobileIosRedirectUri = mobileIosRedirectUri; }

    public String getStudioClientId() { return studioClientId; }
    public void setStudioClientId(String studioClientId) { this.studioClientId = studioClientId; }

    public String getStudioRedirectUri() { return studioRedirectUri; }
    public void setStudioRedirectUri(String studioRedirectUri) { this.studioRedirectUri = studioRedirectUri; }

    public String getStudioClientSecret() { return studioClientSecret; }
    public void setStudioClientSecret(String studioClientSecret) { this.studioClientSecret = studioClientSecret; }

    public String getTokenUri() { return tokenUri; }
    public void setTokenUri(String tokenUri) { this.tokenUri = tokenUri; }

    public String getUserInfoUri() { return userInfoUri; }
    public void setUserInfoUri(String userInfoUri) { this.userInfoUri = userInfoUri; }
}
