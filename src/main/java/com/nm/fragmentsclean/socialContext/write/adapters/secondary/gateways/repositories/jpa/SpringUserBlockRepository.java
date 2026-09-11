package com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa;

import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.entities.UserBlockJpaEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringUserBlockRepository extends JpaRepository<UserBlockJpaEntity, UUID> {
    Optional<UserBlockJpaEntity> findByBlockerIdAndBlockedUserId(UUID blockerId, UUID blockedUserId);
}
