package com.nm.fragmentsclean.authContext.write.businesslogic.gateways;

import com.nm.fragmentsclean.authContext.write.businesslogic.models.Identity;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IdentityRepository {
    Optional<Identity> findByProviderAndProviderUserId(String provider, String providerUserId);

    List<Identity> listByUserId(UUID userId);

    Identity save(Identity identity);
}
