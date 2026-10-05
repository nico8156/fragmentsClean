package com.nm.fragmentsclean.experienceContext.read;
import java.util.Optional;
import org.springframework.stereotype.Component;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;
import com.nm.fragmentsclean.experienceContext.read.projections.ExperienceModerationReportView;
import com.nm.fragmentsclean.experienceContext.read.businesslogic.gateways.ExperienceReadRepository;
@Component public final class GetExperienceModerationReportQueryHandler implements QueryHandler<GetExperienceModerationReportQuery,Optional<ExperienceModerationReportView>> {
 private final ExperienceReadRepository repository;
 public GetExperienceModerationReportQueryHandler(ExperienceReadRepository repository) { this.repository=repository; }
 public Optional<ExperienceModerationReportView> handle(GetExperienceModerationReportQuery query) { return repository.findModerationReport(query.reportId()); }
}
