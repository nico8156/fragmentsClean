package com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthProvider;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthUser;

import java.util.Optional;
import java.util.UUID;

public interface AuthUserRepository {

    Optional<AuthUser> findByProviderAndProviderUserId(AuthProvider provider, String providerUserId);

    AuthUser save(AuthUser user);

    Optional<AuthUser> findById(UUID id);

}
