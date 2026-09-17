package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.storage;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fragments.privacy.erasure-journal")
public class AccountErasureJournalProperties {
  private boolean enabled;
  private String bucket = "";
  private String prefix = "fragments/staging/account-erasure-journal/v1";
  private String region = "eu-west-3";
  private URI endpointOverride;

  public boolean isEnabled() { return enabled; }
  public void setEnabled(boolean enabled) { this.enabled = enabled; }
  public String getBucket() { return bucket; }
  public void setBucket(String bucket) { this.bucket = bucket; }
  public String getPrefix() { return prefix; }
  public void setPrefix(String prefix) { this.prefix = prefix; }
  public String getRegion() { return region; }
  public void setRegion(String region) { this.region = region; }
  public URI getEndpointOverride() { return endpointOverride; }
  public void setEndpointOverride(URI endpointOverride) { this.endpointOverride = endpointOverride; }

  public String requiredBucket() {
    if (bucket == null || bucket.isBlank()) {
      throw new IllegalStateException("fragments.privacy.erasure-journal.bucket is required");
    }
    return bucket.strip();
  }

  public String normalizedPrefix() {
    String value = prefix == null || prefix.isBlank()
        ? "fragments/staging/account-erasure-journal/v1"
        : prefix.strip();
    while (value.startsWith("/")) value = value.substring(1);
    while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
    return value;
  }
}
