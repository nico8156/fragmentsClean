package com.nm.fragmentsclean.socialContext.read;
import java.util.Optional;
import java.util.UUID;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.Query;
import com.nm.fragmentsclean.socialContext.read.projections.ModerationReportView;
public record GetModerationReportQuery(UUID reportId) implements Query<Optional<ModerationReportView>> {
 public GetModerationReportQuery { java.util.Objects.requireNonNull(reportId,"reportId"); }
}
