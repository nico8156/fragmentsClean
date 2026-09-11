package com.nm.fragmentsclean.sharedKernel.businesslogic.media;

import java.time.Duration;

public final class PrivateMediaUrlResolver {
  private final PrivateImageStore store;
  private final Duration ttl;
  public PrivateMediaUrlResolver(PrivateImageStore store, Duration ttl) { this.store=store;this.ttl=ttl; }
  public String resolve(String reference) {
    String key=PrivateMediaReferences.objectKey(reference);
    return key == null ? reference : store.presignDownload(key, ttl).toString();
  }
}
