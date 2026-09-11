package com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.jpa;

import com.nm.fragmentsclean.experienceContext.write.adapters.secondary.repositories.jpa.entities.ExperienceReportJpaEntity;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.gateways.ExperienceReportRepository;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.*;
import java.util.*;

public final class JpaExperienceReportRepository implements ExperienceReportRepository{
    private final SpringExperienceReportRepository repository;public JpaExperienceReportRepository(SpringExperienceReportRepository repository){this.repository=repository;}
    @Override public Optional<ExperienceReport> byId(UUID id){return repository.findByIdForUpdate(id).map(this::domain);}
    @Override public Optional<ExperienceReport> byReporterAndExperience(UUID reporterId,UUID experienceId){return repository.findByReporterIdAndExperienceId(reporterId,experienceId).map(this::domain);}
    @Override public List<ExperienceReport> openByExperience(UUID experienceId){return repository.findByExperienceIdAndStatus(experienceId,ExperienceReportStatus.OPEN).stream().map(this::domain).toList();}
    @Override public void save(ExperienceReport report){repository.save(entity(report));}
    private ExperienceReport domain(ExperienceReportJpaEntity e){return ExperienceReport.fromSnapshot(new ExperienceReport.Snapshot(e.getReportId(),e.getExperienceId(),e.getCoffeeId(),e.getAuthorId(),e.getReporterId(),e.getReason(),e.getDetails(),e.getStatus(),e.getCreatedAt(),e.getResolvedAt(),e.getVersion()));}
    private ExperienceReportJpaEntity entity(ExperienceReport value){var s=value.toSnapshot();return new ExperienceReportJpaEntity(s.reportId(),s.experienceId(),s.coffeeId(),s.authorId(),s.reporterId(),s.reason(),s.details(),s.status(),s.createdAt(),s.resolvedAt(),s.version());}
}
