package com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.fake;

import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.ContentReportRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.ContentReport;
import java.util.*;

public final class FakeContentReportRepository implements ContentReportRepository {
    private final Map<UUID, ContentReport.ContentReportSnapshot> reports = new HashMap<>();
    @Override public Optional<ContentReport> byId(UUID id) { return Optional.ofNullable(reports.get(id)).map(ContentReport::fromSnapshot); }
    @Override public Optional<ContentReport> byReporterAndComment(UUID reporterId, UUID commentId) {
        return reports.values().stream().filter(s -> s.reporterId().equals(reporterId) && s.commentId().equals(commentId))
                .findFirst().map(ContentReport::fromSnapshot);
    }
    @Override public void save(ContentReport report) { reports.put(report.toSnapshot().reportId(), report.toSnapshot()); }
    @Override public List<ContentReport> openByComment(UUID commentId) {
        return reports.values().stream().filter(s -> s.commentId().equals(commentId) && s.status() == com.nm.fragmentsclean.socialContext.write.businesslogic.models.ReportStatus.OPEN)
                .map(ContentReport::fromSnapshot).toList();
    }
    public List<ContentReport.ContentReportSnapshot> allSnapshots() { return List.copyOf(reports.values()); }
}
