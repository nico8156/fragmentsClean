package com.nm.fragmentsclean.socialContext.read;
import java.util.Optional;
import org.springframework.stereotype.Component;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;
import com.nm.fragmentsclean.socialContext.read.projections.ModerationReportView;
import com.nm.fragmentsclean.socialContext.read.AdminModerationReportReadRepository;
@Component public final class GetModerationReportQueryHandler implements QueryHandler<GetModerationReportQuery,Optional<ModerationReportView>> {
 private final AdminModerationReportReadRepository repository;
 public GetModerationReportQueryHandler(AdminModerationReportReadRepository repository) { this.repository=repository; }
 public Optional<ModerationReportView> handle(GetModerationReportQuery query) { return repository.findModerationReport(query.reportId()); }
}
