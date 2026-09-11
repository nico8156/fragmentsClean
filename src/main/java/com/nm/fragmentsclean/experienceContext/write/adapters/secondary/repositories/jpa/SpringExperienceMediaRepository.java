package com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.jpa;

import com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.jpa.entities.ExperienceMediaJpaEntity;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperienceMediaStatus;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface SpringExperienceMediaRepository extends JpaRepository<ExperienceMediaJpaEntity, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select media from experience_media media where media.mediaId=:id")
  Optional<ExperienceMediaJpaEntity> findByIdForUpdate(@Param("id") UUID id);
  Optional<ExperienceMediaJpaEntity> findByMediaId(UUID mediaId);
  @Query("select count(media) from experience_media media where media.experienceId=:experienceId and media.status in :statuses")
  long countActive(@Param("experienceId") UUID experienceId, @Param("statuses") List<ExperienceMediaStatus> statuses);
  boolean existsByExperienceIdAndStatus(UUID experienceId, ExperienceMediaStatus status);
  List<ExperienceMediaJpaEntity> findByExperienceId(UUID experienceId);
  @Query("select media from experience_media media where media.status=:deletionPending or (media.status=:pending and media.updatedAt<:before) order by media.updatedAt")
  List<ExperienceMediaJpaEntity> findCleanupCandidates(@Param("deletionPending") ExperienceMediaStatus deletionPending,
      @Param("pending") ExperienceMediaStatus pending, @Param("before") Instant before,
      org.springframework.data.domain.Pageable page);
}
