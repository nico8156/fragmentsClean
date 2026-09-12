package com.nm.fragmentsclean.sharedKernel.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.storage.SafeImageNormalizer;
import com.nm.fragmentsclean.sharedKernel.businesslogic.media.ImageUploadRejectedException;
import com.nm.fragmentsclean.sharedKernel.businesslogic.media.PrivateImageStore.ImageRules;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class SafeImageNormalizerTest {
  static { System.setProperty("java.awt.headless", "true"); }
  private final SafeImageNormalizer normalizer = new SafeImageNormalizer();

  @Test
  void rejects_oversized_dimensions_from_header_without_decoding_pixel_data() throws Exception {
    var bytes = new ByteArrayOutputStream();
    var output = new DataOutputStream(bytes);
    output.writeLong(0x89504e470d0a1a0aL);
    output.writeInt(13);
    var headerBytes = new ByteArrayOutputStream();
    var header = new DataOutputStream(headerBytes);
    header.writeBytes("IHDR");
    header.writeInt(1000);
    header.writeInt(1000);
    header.write(new byte[] {8, 2, 0, 0, 0});
    output.write(headerBytes.toByteArray());
    var crc = new CRC32();
    crc.update(headerBytes.toByteArray());
    output.writeInt((int) crc.getValue());
    // No pixel payload: attempting a full decode would fail with IMAGE_INVALID.
    assertThatThrownBy(() -> normalizer.normalize(bytes.toByteArray(), "image/png",
        new ImageRules(100_000, 100, 100, 100, false, .8f)))
        .isInstanceOf(ImageUploadRejectedException.class)
        .extracting("code").isEqualTo("IMAGE_PIXEL_LIMIT");
  }

  @Test
  void crops_avatar_to_square_resizes_and_reencodes_as_jpeg() throws Exception {
    var image = new BufferedImage(1200, 600, BufferedImage.TYPE_INT_RGB);
    var graphics = image.createGraphics();
    graphics.setColor(Color.ORANGE);
    graphics.fillRect(0, 0, 1200, 600);
    graphics.dispose();
    var bytes = new ByteArrayOutputStream();
    ImageIO.write(image, "png", bytes);

    var result = normalizer.normalize(
        bytes.toByteArray(), "image/png", new ImageRules(8_000_000, 20_000_000, 512, 512, true, .85f));

    assertThat(result.contentType()).isEqualTo("image/jpeg");
    assertThat(result.width()).isEqualTo(512);
    assertThat(result.height()).isEqualTo(512);
    assertThat(result.sha256()).hasSize(64);
    assertThat(ImageIO.read(new java.io.ByteArrayInputStream(result.bytes()))).isNotNull();
  }

  @Test
  void rejects_declared_type_mismatch_before_domain_attachment() throws Exception {
    var image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
    var bytes = new ByteArrayOutputStream();
    ImageIO.write(image, "png", bytes);

    assertThatThrownBy(() -> normalizer.normalize(
            bytes.toByteArray(), "image/jpeg", new ImageRules(100_000, 1_000, 100, 100, false, .8f)))
        .isInstanceOf(ImageUploadRejectedException.class)
        .extracting("code")
        .isEqualTo("IMAGE_TYPE_MISMATCH");
  }
}
