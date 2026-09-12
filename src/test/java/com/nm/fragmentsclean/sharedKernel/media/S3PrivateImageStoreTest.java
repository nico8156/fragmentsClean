package com.nm.fragmentsclean.sharedKernel.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.storage.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.media.ImageUploadRejectedException;
import com.nm.fragmentsclean.sharedKernel.businesslogic.media.PrivateImageStore.ImageRules;
import java.io.InputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

class S3PrivateImageStoreTest {
  private final S3Client client = mock(S3Client.class);
  private final ImageRules rules = new ImageRules(32, 100, 10, 10, false, .8f);

  @Test
  void rejects_content_length_before_reading_and_aborts_without_draining() {
    var payload = new CountingPayload();
    var aborted = respond(100L, payload);
    assertTooLarge();
    assertThat(payload.readCount).isZero();
    assertThat(aborted).isTrue();
    verify(client, never()).putObject(any(software.amazon.awssdk.services.s3.model.PutObjectRequest.class),
        any(software.amazon.awssdk.core.sync.RequestBody.class));
  }

  @Test
  void bounds_actual_bytes_even_when_content_length_understates_payload() {
    var payload = new CountingPayload();
    var aborted = respond(1L, payload);
    assertTooLarge();
    assertThat(payload.readCount).isEqualTo(33);
    assertThat(aborted).isTrue();
  }

  @Test
  void interrupted_transfer_remains_a_technical_failure() {
    var aborted = respond(1L, new InputStream() {
      public int read() throws IOException { throw new IOException("Interrupted transfer"); }
    });
    assertThatThrownBy(() -> store().normalize("pending", "final", "image/png", rules))
        .isInstanceOf(java.io.UncheckedIOException.class);
    assertThat(aborted).isTrue();
  }

  private void assertTooLarge() {
    assertThatThrownBy(() -> store().normalize("pending", "final", "image/png", rules))
        .isInstanceOf(ImageUploadRejectedException.class)
        .extracting("code").isEqualTo("IMAGE_TOO_LARGE");
  }

  private AtomicBoolean respond(Long size, InputStream payload) {
    var aborted = new AtomicBoolean();
    var stream = new ResponseInputStream<>(GetObjectResponse.builder().contentLength(size).build(),
        AbortableInputStream.create(payload, () -> aborted.set(true)));
    when(client.getObject(any(GetObjectRequest.class))).thenReturn(stream);
    return aborted;
  }

  private S3PrivateImageStore store() {
    var properties = new PrivateImageStorageProperties();
    properties.setBucket("test-private-media");
    return new S3PrivateImageStore(properties, client, null, new SafeImageNormalizer());
  }

  private static class CountingPayload extends InputStream {
    int readCount;
    public int read() { readCount++; return 0; }
  }
}
