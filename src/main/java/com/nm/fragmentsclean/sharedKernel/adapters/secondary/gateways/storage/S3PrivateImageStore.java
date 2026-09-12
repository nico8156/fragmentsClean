package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.storage;

import com.nm.fragmentsclean.sharedKernel.businesslogic.media.PrivateImageStore;
import com.nm.fragmentsclean.sharedKernel.businesslogic.media.ImageUploadRejectedException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

public final class S3PrivateImageStore implements PrivateImageStore {
  private final PrivateImageStorageProperties properties;
  private final S3Client client;
  private final S3Presigner presigner;
  private final SafeImageNormalizer normalizer;

  public S3PrivateImageStore(PrivateImageStorageProperties properties, S3Client client, S3Presigner presigner, SafeImageNormalizer normalizer) {
    this.properties = properties;
    this.client = client;
    this.presigner = presigner;
    this.normalizer = normalizer;
  }

  @Override
  public UploadTarget presignUpload(String objectKey, String contentType, Duration ttl, Instant requestedAt) {
    Duration effectiveTtl = ttl == null ? properties.getUploadTtl() : ttl;
    var put = PutObjectRequest.builder().bucket(properties.requiredBucket()).key(objectKey).contentType(contentType).serverSideEncryption(ServerSideEncryption.AES256).build();
    var signed = presigner.presignPutObject(PutObjectPresignRequest.builder().signatureDuration(effectiveTtl).putObjectRequest(put).build());
    return new UploadTarget(URI.create(signed.url().toString()), "PUT", Map.of("Content-Type", contentType, "x-amz-server-side-encryption", "AES256"), requestedAt.plus(effectiveTtl));
  }

  @Override
  public ProcessedImage normalize(String pendingObjectKey, String finalObjectKey, String declaredContentType, ImageRules rules) {
    var normalized = normalizer.normalize(readBounded(pendingObjectKey, rules.maxInputBytes()), declaredContentType, rules);
    client.putObject(
        PutObjectRequest.builder().bucket(properties.requiredBucket()).key(finalObjectKey).contentType(normalized.contentType()).contentLength((long) normalized.bytes().length).cacheControl("private, max-age=21600").serverSideEncryption(ServerSideEncryption.AES256).build(),
        RequestBody.fromBytes(normalized.bytes()));
    return new ProcessedImage(finalObjectKey, normalized.contentType(), normalized.bytes().length, normalized.width(), normalized.height(), normalized.sha256());
  }

  private byte[] readBounded(String objectKey, long maximumBytes) {
    if (maximumBytes <= 0 || maximumBytes >= Integer.MAX_VALUE) {
      throw new IllegalArgumentException("Invalid image byte limit configuration");
    }
    var response = client.getObject(GetObjectRequest.builder()
        .bucket(properties.requiredBucket()).key(objectKey).build());
    try {
      Long length = response.response().contentLength();
      if (length != null && length > maximumBytes) rejectTooLarge();
      byte[] bytes = response.readNBytes((int) maximumBytes + 1);
      if (bytes.length > maximumBytes) rejectTooLarge();
      return bytes;
    } catch (IOException failure) {
      // A broken transfer is retryable, not an explicit business rejection.
      throw new UncheckedIOException("Unable to read pending image", failure);
    } finally {
      // close() may drain the entire response with the Apache client. Abort
      // instead, including on early size rejection, to bound network work too.
      response.abort();
    }
  }

  private static void rejectTooLarge() {
    throw new ImageUploadRejectedException("IMAGE_TOO_LARGE", "Image exceeds the upload limit");
  }

  @Override
  public URI presignDownload(String objectKey, Duration ttl) {
    Duration effectiveTtl = ttl == null ? properties.getDownloadTtl() : ttl;
    var signed = presigner.presignGetObject(GetObjectPresignRequest.builder().signatureDuration(effectiveTtl).getObjectRequest(builder -> builder.bucket(properties.requiredBucket()).key(objectKey)).build());
    return URI.create(signed.url().toString());
  }

  @Override
  public void delete(String objectKey) {
    if (objectKey == null || objectKey.isBlank()) return;
    try {
      client.deleteObject(DeleteObjectRequest.builder().bucket(properties.requiredBucket()).key(objectKey).build());
    } catch (NoSuchKeyException ignored) {
      // Idempotent by contract.
    }
  }
}
