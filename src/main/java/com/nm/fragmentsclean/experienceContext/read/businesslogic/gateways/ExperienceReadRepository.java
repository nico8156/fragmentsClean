package com.nm.fragmentsclean.experienceContext.read.businesslogic.gateways;

import com.nm.fragmentsclean.experienceContext.read.projections.ExperienceModerationReportView;
import com.nm.fragmentsclean.experienceContext.read.projections.ExperiencePage;
import java.util.List;
import java.util.UUID;

public interface ExperienceReadRepository {
  ExperiencePage listForCoffee(UUID requesterId, UUID coffeeId, String cursor, int limit);

  ExperiencePage listForUser(UUID userId, String cursor, int limit);

  List<ExperienceModerationReportView> listModerationReports(String status, int limit);
}
