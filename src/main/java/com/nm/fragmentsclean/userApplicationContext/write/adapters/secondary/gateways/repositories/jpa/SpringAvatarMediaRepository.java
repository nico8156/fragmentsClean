package com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.jpa;

import com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.jpa.entities.AvatarMediaJpaEntity;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AvatarMediaStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface SpringAvatarMediaRepository extends JpaRepository<AvatarMediaJpaEntity,UUID>{
  @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select media from avatar_media media where media.mediaId=:id") Optional<AvatarMediaJpaEntity> findByIdForUpdate(@Param("id")UUID id);
  Optional<AvatarMediaJpaEntity> findByMediaId(UUID id);
  @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select media from avatar_media media where media.userId=:userId and media.status=:status") Optional<AvatarMediaJpaEntity> findActiveByUser(@Param("userId")UUID userId,@Param("status")AvatarMediaStatus status);
  @Query("select media from avatar_media media where media.status=:deletionPending or (media.status=:pending and media.updatedAt<:before) order by media.updatedAt") List<AvatarMediaJpaEntity> findCleanupCandidates(@Param("deletionPending")AvatarMediaStatus deletionPending,@Param("pending")AvatarMediaStatus pending,@Param("before")Instant before,org.springframework.data.domain.Pageable page);
}
