package com.nm.fragmentsclean.experienceContext.read;
import java.util.Optional;
import java.util.UUID;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.Query;
import com.nm.fragmentsclean.experienceContext.read.projections.ExperienceModerationReportView;
public record GetExperienceModerationReportQuery(UUID reportId) implements Query<Optional<ExperienceModerationReportView>> {
 public GetExperienceModerationReportQuery { java.util.Objects.requireNonNull(reportId,"reportId"); }
}
