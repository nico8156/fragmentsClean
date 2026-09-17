package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.sharedKernel.businesslogic.privacy.AccountErasureJournal;
import java.io.IOException;
import java.util.Objects;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

/** S3 append-only adapter. Existing keys are accepted only when they identify the same request. */
public final class S3AccountErasureJournal implements AccountErasureJournal {
  private final AccountErasureJournalProperties properties;
  private final S3Client client;
  private final ObjectMapper objectMapper;

  public S3AccountErasureJournal(
      AccountErasureJournalProperties properties, S3Client client, ObjectMapper objectMapper) {
    this.properties = properties;
    this.client = client;
    this.objectMapper = objectMapper;
  }

  @Override
  public void record(Entry entry) {
    var key = key(entry);
    byte[] payload;
    try {
      payload = objectMapper.writeValueAsBytes(StoredEntry.from(entry));
    } catch (IOException failure) {
      throw new IllegalStateException("Cannot serialize account erasure journal entry", failure);
    }
    try {
      client.putObject(
          PutObjectRequest.builder()
              .bucket(properties.requiredBucket())
              .key(key)
              .contentType("application/json")
              .serverSideEncryption(ServerSideEncryption.AES256)
              .ifNoneMatch("*")
              .build(),
          RequestBody.fromBytes(payload));
    } catch (S3Exception failure) {
      if (failure.statusCode() != 412) throw failure;
      verifyExistingEntry(key, entry);
    }
  }

  private void verifyExistingEntry(String key, Entry expected) {
    try {
      var bytes = client.getObjectAsBytes(
          GetObjectRequest.builder().bucket(properties.requiredBucket()).key(key).build());
      var actual = objectMapper.readValue(bytes.asByteArray(), StoredEntry.class);
      if (actual.version() != 1
          || !Objects.equals(actual.requestId(), expected.requestId().toString())
          || !Objects.equals(actual.userId(), expected.userId().toString())
          || !Objects.equals(actual.authUserId(), expected.authUserId().toString())) {
        throw new IllegalStateException("Account erasure journal key collision: " + key);
      }
    } catch (IOException failure) {
      throw new IllegalStateException("Cannot verify existing account erasure journal entry", failure);
    }
  }

  private String key(Entry entry) {
    return properties.normalizedPrefix() + "/" + entry.userId() + "/" + entry.requestId() + ".json";
  }

  private record StoredEntry(
      int version, String requestId, String userId, String authUserId, String requestedAt) {
    private static StoredEntry from(Entry entry) {
      return new StoredEntry(
          1,
          entry.requestId().toString(),
          entry.userId().toString(),
          entry.authUserId().toString(),
          entry.requestedAt().toString());
    }
  }
}
