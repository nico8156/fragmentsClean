package com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.apple;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "apple.oauth")
public class AppleOAuthProperties {
  private String clientId;
  private String teamId;
  private String keyId;
  private String privateKey;
  private String issuer = "https://appleid.apple.com";
  private String jwkSetUri = "https://appleid.apple.com/auth/keys";
  private String tokenUri = "https://appleid.apple.com/auth/token";
  private String revokeUri = "https://appleid.apple.com/auth/revoke";

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String v) {
    clientId = v;
  }

  public String getTeamId() {
    return teamId;
  }

  public void setTeamId(String v) {
    teamId = v;
  }

  public String getKeyId() {
    return keyId;
  }

  public void setKeyId(String v) {
    keyId = v;
  }

  public String getPrivateKey() {
    return privateKey;
  }

  public void setPrivateKey(String v) {
    privateKey = v;
  }

  public String getIssuer() {
    return issuer;
  }

  public void setIssuer(String v) {
    issuer = v;
  }

  public String getJwkSetUri() {
    return jwkSetUri;
  }

  public void setJwkSetUri(String v) {
    jwkSetUri = v;
  }

  public String getTokenUri() {
    return tokenUri;
  }

  public void setTokenUri(String v) {
    tokenUri = v;
  }

  public String getRevokeUri() {
    return revokeUri;
  }

  public void setRevokeUri(String v) {
    revokeUri = v;
  }
}
