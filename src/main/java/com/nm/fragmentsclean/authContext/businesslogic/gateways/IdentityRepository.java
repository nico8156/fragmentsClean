package com.nm.fragmentsclean.authContext.businesslogic.gateways;

import com.nm.fragmentsclean.authContext.businesslogic.models.Identity;
import com.nm.fragmentsclean.authContext.businesslogic.models.VerifiedOAuthProfile;


import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IdentityRepository {
    Optional<Identity> findByProviderAndProviderUserId(String provider, String providerUserId);

    List<Identity> listByUserId(UUID userId);

    Identity save(Identity identity);
}
