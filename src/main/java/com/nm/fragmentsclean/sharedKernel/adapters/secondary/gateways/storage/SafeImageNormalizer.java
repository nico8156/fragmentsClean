package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.storage;

import com.nm.fragmentsclean.sharedKernel.businesslogic.media.ImageUploadRejectedException;
import com.nm.fragmentsclean.sharedKernel.businesslogic.media.PrivateImageStore.ImageRules;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;

public final class SafeImageNormalizer {
  public Normalized normalize(byte[] input, String declaredContentType, ImageRules rules) {
    if (input == null || input.length == 0) reject("IMAGE_EMPTY", "Image is empty");
    if (input.length > rules.maxInputBytes()) reject("IMAGE_TOO_LARGE", "Image exceeds the upload limit");
    String actualType = detect(input);
    if (!actualType.equals(normalizeContentType(declaredContentType))) {
      reject("IMAGE_TYPE_MISMATCH", "Declared and actual image types do not match");
    }

    BufferedImage source;
    try {
      source = ImageIO.read(new ByteArrayInputStream(input));
    } catch (Exception failure) {
      throw new ImageUploadRejectedException("IMAGE_INVALID", "Image cannot be decoded");
    }
    if (source == null || source.getWidth() <= 0 || source.getHeight() <= 0) reject("IMAGE_INVALID", "Image cannot be decoded");
    if ((long) source.getWidth() * source.getHeight() > rules.maxPixels()) reject("IMAGE_PIXEL_LIMIT", "Image dimensions exceed the safety limit");

    BufferedImage transformed = transform(source, rules);
    byte[] output = encodeJpeg(transformed, rules.jpegQuality());
    return new Normalized(output, "image/jpeg", transformed.getWidth(), transformed.getHeight(), sha256(output));
  }

  private BufferedImage transform(BufferedImage source, ImageRules rules) {
    int sourceX = 0;
    int sourceY = 0;
    int sourceWidth = source.getWidth();
    int sourceHeight = source.getHeight();
    if (rules.squareCrop()) {
      int side = Math.min(sourceWidth, sourceHeight);
      sourceX = (sourceWidth - side) / 2;
      sourceY = (sourceHeight - side) / 2;
      sourceWidth = side;
      sourceHeight = side;
    }
    double ratio = Math.min(1d, Math.min((double) rules.maxWidth() / sourceWidth, (double) rules.maxHeight() / sourceHeight));
    int width = Math.max(1, (int) Math.round(sourceWidth * ratio));
    int height = Math.max(1, (int) Math.round(sourceHeight * ratio));
    var output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = output.createGraphics();
    try {
      graphics.setColor(Color.WHITE);
      graphics.fillRect(0, 0, width, height);
      graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      graphics.drawImage(source, 0, 0, width, height, sourceX, sourceY, sourceX + sourceWidth, sourceY + sourceHeight, null);
    } finally {
      graphics.dispose();
    }
    return output;
  }

  private byte[] encodeJpeg(BufferedImage image, float requestedQuality) {
    float quality = Math.max(0.5f, Math.min(requestedQuality, 0.95f));
    try {
      var writers = ImageIO.getImageWritersByFormatName("jpeg");
      if (!writers.hasNext()) reject("IMAGE_ENCODER_UNAVAILABLE", "JPEG encoder is unavailable");
      var writer = writers.next();
      var output = new ByteArrayOutputStream();
      try (var stream = ImageIO.createImageOutputStream(output)) {
        writer.setOutput(stream);
        ImageWriteParam params = writer.getDefaultWriteParam();
        params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        params.setCompressionQuality(quality);
        writer.write(null, new IIOImage(image, null, null), params);
      } finally {
        writer.dispose();
      }
      return output.toByteArray();
    } catch (ImageUploadRejectedException rejected) {
      throw rejected;
    } catch (Exception failure) {
      throw new ImageUploadRejectedException("IMAGE_ENCODING_FAILED", "Image cannot be normalized");
    }
  }

  private static String detect(byte[] bytes) {
    if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) return "image/jpeg";
    if (bytes.length >= 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47 && bytes[4] == 0x0d && bytes[5] == 0x0a && bytes[6] == 0x1a && bytes[7] == 0x0a) return "image/png";
    throw new ImageUploadRejectedException("IMAGE_TYPE_UNSUPPORTED", "Only JPEG and PNG are supported");
  }

  private static String normalizeContentType(String value) {
    if (value == null) return "";
    String normalized = value.strip().toLowerCase(java.util.Locale.ROOT);
    return "image/jpg".equals(normalized) ? "image/jpeg" : normalized;
  }

  private static String sha256(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (Exception impossible) {
      throw new IllegalStateException("SHA-256 unavailable", impossible);
    }
  }

  private static void reject(String code, String message) { throw new ImageUploadRejectedException(code, message); }
  public record Normalized(byte[] bytes, String contentType, int width, int height, String sha256) {}
}
