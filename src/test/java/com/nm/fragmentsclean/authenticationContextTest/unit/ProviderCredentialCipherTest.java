package com.nm.fragmentsclean.authenticationContextTest.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.credentials.ProviderCredentialCipher;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class ProviderCredentialCipherTest {
  @Test
  void encrypts_with_randomized_authenticated_encryption_and_round_trips() {
    String key = Base64.getEncoder().encodeToString(new byte[32]);
    var cipher = new ProviderCredentialCipher(key);
    String first = cipher.encrypt("apple-refresh-token");
    String second = cipher.encrypt("apple-refresh-token");
    assertThat(first).isNotEqualTo(second);
    assertThat(cipher.decrypt(first)).isEqualTo("apple-refresh-token");
  }
}
