package com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.repositories.jpa;

import com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.repositories.jpa.entities.RefreshTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringRefreshTokenRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {

    Optional<RefreshTokenJpaEntity> findByToken(String token);
}
