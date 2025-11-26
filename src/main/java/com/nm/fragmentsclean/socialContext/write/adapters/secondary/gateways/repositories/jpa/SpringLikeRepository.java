package com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa;

import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.entities.LikeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringLikeRepository extends JpaRepository<LikeJpaEntity, UUID> {

    Optional<LikeJpaEntity> findByUserIdAndTargetId(UUID userId, UUID targetId);
    long countByTargetIdAndActiveTrue(UUID targetId);

    boolean existsByTargetIdAndUserIdAndActiveTrue(UUID targetId, UUID userId);
    long countByTargetIdAndActiveIsTrue(UUID targetId);
    List<LikeJpaEntity> findByTargetId(UUID targetId);
}
