package com.nm.fragmentsclean.sharedKernel.privacy;

import static org.assertj.core.api.Assertions.assertThat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.storage.AccountErasureJournalProperties;
import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.storage.S3AccountErasureJournal;
import com.nm.fragmentsclean.sharedKernel.businesslogic.privacy.AccountErasureJournal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

class S3AccountErasureJournalTest {
  @Test
  void writes_an_encrypted_create_only_marker_under_the_owned_user_prefix() {
    var client = new RecordingS3Client();
    var properties = new AccountErasureJournalProperties();
    properties.setBucket("erasure-journal");
    properties.setPrefix("fragments/prod/account-erasure-journal/v1");
    var journal = new S3AccountErasureJournal(properties, client, new ObjectMapper().findAndRegisterModules());
    var requestId = UUID.fromString("22222222-2222-4222-8222-222222222222");
    var userId = UUID.fromString("11111111-1111-4111-8111-111111111111");

    journal.record(new AccountErasureJournal.Entry(
        requestId, userId, userId, Instant.parse("2026-09-17T10:00:00Z")));

    assertThat(client.request.bucket()).isEqualTo("erasure-journal");
    assertThat(client.request.key()).isEqualTo(
        "fragments/prod/account-erasure-journal/v1/" + userId + "/" + requestId + ".json");
    assertThat(client.request.ifNoneMatch()).isEqualTo("*");
    assertThat(client.request.serverSideEncryptionAsString()).isEqualTo("AES256");
    assertThat(new String(client.payload, java.nio.charset.StandardCharsets.UTF_8))
        .contains("\"version\":1")
        .contains("\"requestedAt\":\"2026-09-17T10:00:00Z\"");

    client.rejectNextCreate = true;
    journal.record(new AccountErasureJournal.Entry(
        requestId, userId, userId, Instant.parse("2026-09-17T10:01:00Z")));
  }

  private static final class RecordingS3Client implements S3Client {
    private PutObjectRequest request;
    private byte[] payload;
    private boolean rejectNextCreate;
    @Override public PutObjectResponse putObject(PutObjectRequest request, RequestBody body) {
      if (rejectNextCreate) {
        rejectNextCreate = false;
        throw S3Exception.builder().statusCode(412).message("precondition failed").build();
      }
      this.request = request;
      try (var input = body.contentStreamProvider().newStream()) {
        this.payload = input.readAllBytes();
      } catch (java.io.IOException failure) {
        throw new IllegalStateException(failure);
      }
      return PutObjectResponse.builder().build();
    }
    @Override public ResponseBytes<GetObjectResponse> getObjectAsBytes(GetObjectRequest request) {
      return ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), payload);
    }
    @Override public String serviceName() { return "s3"; }
    @Override public void close() {}
  }
}
