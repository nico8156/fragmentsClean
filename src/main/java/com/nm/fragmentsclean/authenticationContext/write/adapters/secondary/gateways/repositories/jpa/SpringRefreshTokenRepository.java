package com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.repositories.jpa;

import com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.repositories.jpa.entities.RefreshTokenJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringRefreshTokenRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {

  Optional<RefreshTokenJpaEntity> findByToken(String token);

  List<RefreshTokenJpaEntity> findAllByUserId(UUID userId);
}
