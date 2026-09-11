package com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.jpa;

import com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.jpa.entities.ExperienceJpaEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface SpringExperienceRepository extends JpaRepository<ExperienceJpaEntity,UUID>{
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select experience from experiences experience where experience.experienceId=:id")
    Optional<ExperienceJpaEntity> findByIdForUpdate(@Param("id") UUID id);
}
