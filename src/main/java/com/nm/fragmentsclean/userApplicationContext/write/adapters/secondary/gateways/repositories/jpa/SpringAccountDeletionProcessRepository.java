package com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.jpa;

import com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.jpa.entities.AccountDeletionProcessJpaEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringAccountDeletionProcessRepository
    extends JpaRepository<AccountDeletionProcessJpaEntity, UUID> {
  Optional<AccountDeletionProcessJpaEntity> findByUserId(UUID userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select process from AccountDeletionProcessJpaEntity process where process.requestId ="
          + " :requestId")
  Optional<AccountDeletionProcessJpaEntity> findByRequestIdForUpdate(
      @Param("requestId") UUID requestId);
}
