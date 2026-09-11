package com.nm.fragmentsclean.socialContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.AggregateRoot;
import java.time.Instant;
import java.util.UUID;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;

public final class ContentReport extends AggregateRoot {
    private final UUID commentId;
    private final UUID targetId;
    private final UUID authorId;
    private final UUID reporterId;
    private final ReportReason reason;
    private final String details;
    private ReportStatus status;
    private final Instant createdAt;
    private Instant resolvedAt;
    private long version;

    private ContentReport(ContentReportSnapshot snapshot) {
        super(snapshot.reportId());
        commentId = snapshot.commentId();
        targetId = snapshot.targetId();
        authorId = snapshot.authorId();
        reporterId = snapshot.reporterId();
        reason = snapshot.reason();
        details = snapshot.details();
        status = snapshot.status();
        createdAt = snapshot.createdAt();
        resolvedAt = snapshot.resolvedAt();
        version = snapshot.version();
    }

    public static ContentReport create(UUID reportId, UUID commentId, UUID targetId, UUID authorId,
                                       UUID reporterId, ReportReason reason, String details, Instant now) {
        if (reporterId.equals(authorId)) throw new IllegalArgumentException("Own comment cannot be reported");
        return new ContentReport(new ContentReportSnapshot(reportId, commentId, targetId, authorId,
                reporterId, reason, normalizeDetails(details), ReportStatus.OPEN, now, null, 0));
    }

    public static ContentReport fromSnapshot(ContentReportSnapshot snapshot) {
        return new ContentReport(snapshot);
    }

    public void registerCreatedEvent(UUID commandId, Instant clientAt, Instant now) {
        registerEvent(new CommentReportedEvent(UUID.randomUUID(), commandId, id, commentId, targetId,
                authorId, reporterId, reason, details, status, version, now, clientAt));
    }

    public boolean resolve(ReportStatus decision, Instant now) {
        if (decision == ReportStatus.OPEN) throw new IllegalArgumentException("A moderation decision must close the report");
        if (status == decision) return false;
        status = decision;
        resolvedAt = now;
        version++;
        return true;
    }

    public ContentReportSnapshot toSnapshot() {
        return new ContentReportSnapshot(id, commentId, targetId, authorId, reporterId, reason,
                details, status, createdAt, resolvedAt, version);
    }

    private static String normalizeDetails(String details) {
        if (details == null || details.isBlank()) return null;
        String value = details.strip();
        if (value.length() > 1000) throw new BusinessCommandRejectedException("REPORT_DETAILS_TOO_LONG", "Report details exceed 1000 characters");
        return value;
    }

    public record ContentReportSnapshot(UUID reportId, UUID commentId, UUID targetId, UUID authorId,
                                        UUID reporterId, ReportReason reason, String details,
                                        ReportStatus status, Instant createdAt, Instant resolvedAt,
                                        long version) { }
}
