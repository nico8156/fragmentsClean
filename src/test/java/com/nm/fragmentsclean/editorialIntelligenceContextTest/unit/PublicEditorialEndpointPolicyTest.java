package com.nm.fragmentsclean.editorialIntelligenceContextTest.unit;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.adapters.secondary.gateways.http.PublicEditorialEndpointPolicy;
import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.EditorialSourceDiscoveryException;
import java.net.InetAddress;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class PublicEditorialEndpointPolicyTest {
  @Test
  void detects_a_hostname_that_rebinds_from_a_public_to_a_private_address() throws Exception {
    var resolution = new AtomicInteger();
    var publicAddress = InetAddress.getByAddress(new byte[] {8, 8, 8, 8});
    var privateAddress = InetAddress.getByAddress(new byte[] {127, 0, 0, 1});
    var policy = new PublicEditorialEndpointPolicy(
        Set.of(), ignored -> resolution.getAndIncrement() == 0
            ? new InetAddress[] {publicAddress}
            : new InetAddress[] {privateAddress});
    var endpoint = URI.create("https://feed.example.test/articles.xml");

    assertThatNoException().isThrownBy(() -> policy.validate(endpoint));
    assertThatThrownBy(() -> policy.validate(endpoint))
        .isInstanceOf(EditorialSourceDiscoveryException.class)
        .extracting(failure -> ((EditorialSourceDiscoveryException) failure).category())
        .isEqualTo(EditorialSourceDiscoveryException.Category.MALFORMED_ENDPOINT);
  }

  @Test
  void rejects_credentials_embedded_in_an_operator_endpoint() {
    var policy = new PublicEditorialEndpointPolicy(Set.of());

    assertThatThrownBy(() -> policy.validate("https://user:secret@example.test/feed"))
        .isInstanceOf(EditorialSourceDiscoveryException.class)
        .extracting(failure -> ((EditorialSourceDiscoveryException) failure).category())
        .isEqualTo(EditorialSourceDiscoveryException.Category.MALFORMED_ENDPOINT);
  }
}
