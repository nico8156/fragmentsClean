package com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.jpa;


import com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.jpa.entities.AppUserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface SpringAppUserRepository extends JpaRepository<AppUserJpaEntity, UUID> {

    Optional<AppUserJpaEntity> findByAuthUserId(UUID authUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from AppUserJpaEntity user where user.id=:id")
    Optional<AppUserJpaEntity> findByIdForUpdate(@Param("id") UUID id);
}
