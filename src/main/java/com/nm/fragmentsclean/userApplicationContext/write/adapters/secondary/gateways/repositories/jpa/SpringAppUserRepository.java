package com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.jpa;


import com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.jpa.entities.AppUserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringAppUserRepository extends JpaRepository<AppUserJpaEntity, UUID> {

    Optional<AppUserJpaEntity> findByAuthUserId(UUID authUserId);
}
