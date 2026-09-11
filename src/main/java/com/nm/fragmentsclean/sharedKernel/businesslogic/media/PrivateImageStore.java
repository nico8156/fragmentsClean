package com.nm.fragmentsclean.sharedKernel.businesslogic.media;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/** Technical object-storage boundary. Business ownership remains in the calling context. */
public interface PrivateImageStore {
  UploadTarget presignUpload(String objectKey, String contentType, Duration ttl, Instant requestedAt);
  ProcessedImage normalize(String pendingObjectKey, String finalObjectKey, String declaredContentType, ImageRules rules);
  URI presignDownload(String objectKey, Duration ttl);
  void delete(String objectKey);

  record UploadTarget(URI url, String method, Map<String, String> headers, Instant expiresAt) {}
  record ProcessedImage(String objectKey, String contentType, long size, int width, int height, String sha256) {}
  record ImageRules(long maxInputBytes, long maxPixels, int maxWidth, int maxHeight, boolean squareCrop, float jpegQuality) {}
}
