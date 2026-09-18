package com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.repositories.jpa;

import com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.repositories.jpa.entities.RefreshTokenJpaEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface SpringRefreshTokenRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {

  Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);

  @Query("select token.familyId from RefreshTokenJpaEntity token where token.tokenHash = :tokenHash")
  Optional<UUID> findFamilyIdByTokenHash(@Param("tokenHash") String tokenHash);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select token from RefreshTokenJpaEntity token where token.tokenHash = :tokenHash")
  Optional<RefreshTokenJpaEntity> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<RefreshTokenJpaEntity> findFirstByFamilyIdOrderById(UUID familyId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("""
      select familyToken from RefreshTokenJpaEntity familyToken
      where familyToken.familyId = (
        select presented.familyId from RefreshTokenJpaEntity presented
        where presented.tokenHash = :tokenHash
      )
      order by familyToken.id
      """)
  List<RefreshTokenJpaEntity> findFamilyByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

  List<RefreshTokenJpaEntity> findAllByUserId(UUID userId);
}
