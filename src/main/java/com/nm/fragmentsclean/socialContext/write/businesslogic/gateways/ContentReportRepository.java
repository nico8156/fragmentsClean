package com.nm.fragmentsclean.socialContext.write.businesslogic.gateways;

import com.nm.fragmentsclean.socialContext.write.businesslogic.models.ContentReport;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface ContentReportRepository {
    Optional<ContentReport> byId(UUID reportId);
    Optional<ContentReport> byReporterAndComment(UUID reporterId, UUID commentId);
    List<ContentReport> openByComment(UUID commentId);
    void save(ContentReport report);
}
