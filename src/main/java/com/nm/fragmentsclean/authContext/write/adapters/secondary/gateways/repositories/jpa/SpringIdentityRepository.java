package com.nm.fragmentsclean.authContext.write.adapters.secondary.gateways.repositories.jpa;


import com.nm.fragmentsclean.authContext.write.adapters.secondary.gateways.repositories.jpa.entities.IdentityJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringIdentityRepository extends JpaRepository<IdentityJpaEntity, UUID> {

    Optional<IdentityJpaEntity> findByProviderAndProviderUserId(String provider, String providerUserId);

    List<IdentityJpaEntity> findByUserId(UUID userId);
}
