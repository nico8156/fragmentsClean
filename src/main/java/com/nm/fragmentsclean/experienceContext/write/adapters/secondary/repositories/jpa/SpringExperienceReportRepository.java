package com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.jpa;

import com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.jpa.entities.ExperienceReportJpaEntity;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperienceReportStatus;
import jakarta.persistence.LockModeType;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface SpringExperienceReportRepository extends JpaRepository<ExperienceReportJpaEntity,UUID>{
    @Lock(LockModeType.PESSIMISTIC_WRITE)@Query("select report from experience_reports report where report.reportId=:id")Optional<ExperienceReportJpaEntity> findByIdForUpdate(@Param("id")UUID id);
    Optional<ExperienceReportJpaEntity> findByReporterIdAndExperienceId(UUID reporterId,UUID experienceId);
    List<ExperienceReportJpaEntity> findByExperienceIdAndStatus(UUID experienceId,ExperienceReportStatus status);
}
