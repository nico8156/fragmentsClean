package com.nm.fragmentsclean.socialContext.read;
import java.util.Optional;
import java.util.UUID;
import com.nm.fragmentsclean.socialContext.read.projections.ModerationReportView;
public interface AdminModerationReportReadRepository {
 Optional<ModerationReportView> findModerationReport(UUID reportId);
}
