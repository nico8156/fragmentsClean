package com.nm.fragmentsclean.editorialIntelligenceContext.write.adapters.secondary.gateways.http;

import com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.gateways.EditorialSourceDiscoveryException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Restricts operator-provided editorial endpoints to public HTTP destinations.
 * Exact host exceptions exist for controlled local/test deployments only.
 */
public final class PublicEditorialEndpointPolicy {
  private final Set<String> explicitlyAllowedHosts;
  private final AddressResolver addressResolver;

  public PublicEditorialEndpointPolicy(Set<String> explicitlyAllowedHosts) {
    this(explicitlyAllowedHosts, InetAddress::getAllByName);
  }

  public PublicEditorialEndpointPolicy(
      Set<String> explicitlyAllowedHosts, AddressResolver addressResolver) {
    this.explicitlyAllowedHosts =
        Objects.requireNonNull(explicitlyAllowedHosts, "explicitlyAllowedHosts").stream()
            .map(host -> host.toLowerCase(Locale.ROOT).trim())
            .filter(host -> !host.isBlank())
            .collect(Collectors.toUnmodifiableSet());
    this.addressResolver = Objects.requireNonNull(addressResolver, "addressResolver");
  }

  public URI validate(String endpoint) {
    final URI uri;
    try {
      uri = URI.create(endpoint);
    } catch (RuntimeException invalid) {
      throw EditorialSourceDiscoveryException.malformedEndpoint(
          "Invalid editorial source endpoint", invalid);
    }
    validate(uri);
    return uri;
  }

  public void validate(URI uri) {
    var scheme = uri.getScheme();
    var host = uri.getHost();
    if ((!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme))
        || host == null
        || host.isBlank()
        || uri.getUserInfo() != null
        || uri.getFragment() != null) {
      throw EditorialSourceDiscoveryException.malformedEndpoint(
          "Editorial source endpoint must be an absolute HTTP(S) URI without credentials or fragment",
          new IllegalArgumentException("Unsafe editorial source URI"));
    }

    var normalizedHost = host.toLowerCase(Locale.ROOT);
    if (explicitlyAllowedHosts.contains(normalizedHost)) {
      return;
    }

    final InetAddress[] addresses;
    try {
      addresses = addressResolver.resolve(host);
    } catch (UnknownHostException unresolved) {
      throw EditorialSourceDiscoveryException.remoteFailure(
          "Editorial source host could not be resolved", unresolved);
    }
    if (addresses.length == 0) {
      throw EditorialSourceDiscoveryException.remoteFailure(
          "Editorial source host resolved to no address");
    }
    for (var address : addresses) {
      if (!isPublic(address)) {
        throw EditorialSourceDiscoveryException.malformedEndpoint(
            "Editorial source endpoint resolves to a non-public address",
            new IllegalArgumentException("Non-public destination"));
      }
    }
  }

  private static boolean isPublic(InetAddress address) {
    if (address.isAnyLocalAddress()
        || address.isLoopbackAddress()
        || address.isLinkLocalAddress()
        || address.isSiteLocalAddress()
        || address.isMulticastAddress()) {
      return false;
    }
    byte[] bytes = address.getAddress();
    if (address instanceof Inet6Address) {
      int first = Byte.toUnsignedInt(bytes[0]);
      return (first & 0xfe) != 0xfc;
    }
    int first = Byte.toUnsignedInt(bytes[0]);
    int second = Byte.toUnsignedInt(bytes[1]);
    return first != 0
        && !(first == 100 && second >= 64 && second <= 127)
        && !(first == 192 && second == 0)
        && !(first == 198 && (second == 18 || second == 19))
        && first < 224;
  }

  @FunctionalInterface
  public interface AddressResolver {
    InetAddress[] resolve(String host) throws UnknownHostException;
  }
}
