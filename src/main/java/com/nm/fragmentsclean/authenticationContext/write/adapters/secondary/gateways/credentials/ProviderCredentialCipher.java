package com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.credentials;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;

public final class ProviderCredentialCipher {
  private final String encodedKey;
  private final SecureRandom random = new SecureRandom();

  public ProviderCredentialCipher(
      @Value("${auth.provider-credential-encryption-key:}") String encodedKey) {
    this.encodedKey = encodedKey;
  }

  public String encrypt(String plaintext) {
    try {
      byte[] iv = new byte[12];
      random.nextBytes(iv);
      var cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, iv));
      byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
      return Base64.getEncoder()
          .encodeToString(
              ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array());
    } catch (Exception e) {
      throw new IllegalStateException("Cannot encrypt provider credential", e);
    }
  }

  public String decrypt(String encoded) {
    try {
      byte[] all = Base64.getDecoder().decode(encoded);
      byte[] iv = new byte[12];
      byte[] encrypted = new byte[all.length - 12];
      System.arraycopy(all, 0, iv, 0, 12);
      System.arraycopy(all, 12, encrypted, 0, encrypted.length);
      var cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
      return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalStateException("Cannot decrypt provider credential", e);
    }
  }

  private SecretKeySpec key() {
    if (encodedKey == null || encodedKey.isBlank())
      throw new IllegalStateException("auth.provider-credential-encryption-key is required");
    byte[] bytes = Base64.getDecoder().decode(encodedKey);
    if (bytes.length != 32)
      throw new IllegalStateException("Provider credential key must be 32 bytes");
    return new SecretKeySpec(bytes, "AES");
  }
}
