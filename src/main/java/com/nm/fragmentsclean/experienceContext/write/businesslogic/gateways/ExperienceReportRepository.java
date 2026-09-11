package com.nm.fragmentsclean.experienceContext.write.businesslogic.gateways;

import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperienceReport;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExperienceReportRepository {
    Optional<ExperienceReport> byId(UUID reportId);
    Optional<ExperienceReport> byReporterAndExperience(UUID reporterId, UUID experienceId);
    List<ExperienceReport> openByExperience(UUID experienceId);
    void save(ExperienceReport report);
}
