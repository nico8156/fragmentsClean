package com.nm.fragmentsclean.experienceContext.read;

import com.nm.fragmentsclean.experienceContext.read.businesslogic.gateways.ExperienceReadRepository;
import com.nm.fragmentsclean.experienceContext.read.projections.ExperienceModerationReportView;
import java.util.List;

public final class ListExperienceModerationReportsQueryHandler {
  private final ExperienceReadRepository repository;

  public ListExperienceModerationReportsQueryHandler(ExperienceReadRepository repository) {
    this.repository = repository;
  }

  public List<ExperienceModerationReportView> handle(String status, int limit) {
    return repository.listModerationReports(status, limit);
  }
}
