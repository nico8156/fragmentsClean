package com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa;

import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.entities.ContentReportJpaEntity;
import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.ContentReportRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.ContentReport;
import java.util.Optional;
import java.util.UUID;
import java.util.List;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.ReportStatus;

public final class JpaContentReportRepository implements ContentReportRepository {
    private final SpringContentReportRepository repository;
    public JpaContentReportRepository(SpringContentReportRepository repository) { this.repository = repository; }
    @Override public Optional<ContentReport> byId(UUID id) { return repository.findById(id).map(this::toDomain); }
    @Override public Optional<ContentReport> byReporterAndComment(UUID reporterId, UUID commentId) {
        return repository.findByReporterIdAndCommentId(reporterId, commentId).map(this::toDomain);
    }
    @Override public List<ContentReport> openByComment(UUID commentId) {
        return repository.findByCommentIdAndStatus(commentId, ReportStatus.OPEN).stream().map(this::toDomain).toList();
    }
    @Override public void save(ContentReport report) { repository.save(toJpa(report)); }
    private ContentReport toDomain(ContentReportJpaEntity e) {
        return ContentReport.fromSnapshot(new ContentReport.ContentReportSnapshot(e.getReportId(), e.getCommentId(),
                e.getTargetId(), e.getAuthorId(), e.getReporterId(), e.getReason(), e.getDetails(), e.getStatus(),
                e.getCreatedAt(), e.getResolvedAt(), e.getVersion()));
    }
    private ContentReportJpaEntity toJpa(ContentReport report) {
        var s = report.toSnapshot();
        return new ContentReportJpaEntity(s.reportId(), s.commentId(), s.targetId(), s.authorId(), s.reporterId(),
                s.reason(), s.details(), s.status(), s.createdAt(), s.resolvedAt(), s.version());
    }
}
