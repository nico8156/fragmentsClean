package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.storage;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fragments.media.storage")
public class PrivateImageStorageProperties {
  private String bucket = "";
  private String prefix = "fragments/staging/private-media";
  private String region = "eu-west-3";
  private URI endpointOverride;
  private Duration uploadTtl = Duration.ofMinutes(10);
  private Duration downloadTtl = Duration.ofHours(6);

  public String getBucket() { return bucket; }
  public void setBucket(String bucket) { this.bucket = bucket; }
  public String getPrefix() { return prefix; }
  public void setPrefix(String prefix) { this.prefix = prefix; }
  public String getRegion() { return region; }
  public void setRegion(String region) { this.region = region; }
  public URI getEndpointOverride() { return endpointOverride; }
  public void setEndpointOverride(URI endpointOverride) { this.endpointOverride = endpointOverride; }
  public Duration getUploadTtl() { return uploadTtl; }
  public void setUploadTtl(Duration uploadTtl) { this.uploadTtl = uploadTtl; }
  public Duration getDownloadTtl() { return downloadTtl; }
  public void setDownloadTtl(Duration downloadTtl) { this.downloadTtl = downloadTtl; }

  public String normalizedPrefix() {
    String value = prefix == null || prefix.isBlank() ? "fragments/staging/private-media" : prefix.strip();
    while (value.startsWith("/")) value = value.substring(1);
    while (value.endsWith("/")) value = value.substring(0, value.length() - 1);
    return value;
  }

  public String requiredBucket() {
    if (bucket == null || bucket.isBlank()) throw new IllegalStateException("fragments.media.storage.bucket is required");
    return bucket.strip();
  }
}
