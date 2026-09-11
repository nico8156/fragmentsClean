package com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthProvider;
import java.util.Optional;
import java.util.UUID;

public interface ProviderCredentialRepository {
  void save(UUID userId, AuthProvider provider, String refreshToken);

  Optional<String> findRefreshToken(UUID userId, AuthProvider provider);

  void delete(UUID userId, AuthProvider provider);
}
