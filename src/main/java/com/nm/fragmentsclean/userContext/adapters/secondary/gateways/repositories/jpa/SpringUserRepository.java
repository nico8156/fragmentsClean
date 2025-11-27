package com.nm.fragmentsclean.userContext.adapters.secondary.gateways.repositories.jpa;

import com.nm.fragmentsclean.userContext.adapters.secondary.gateways.repositories.jpa.entities.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringUserRepository extends JpaRepository<UserJpaEntity, UUID> {
    // plus tard : findByEmail(...), etc.
}
