package com.nm.fragmentsclean.experienceContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.AggregateRoot;
import java.time.Instant;
import java.util.UUID;

public final class ExperienceReport extends AggregateRoot {
    private final UUID experienceId; private final UUID coffeeId; private final UUID authorId;
    private final UUID reporterId; private final ExperienceReportReason reason; private final String details;
    private final Instant createdAt; private ExperienceReportStatus status; private Instant resolvedAt;
    private long version;

    private ExperienceReport(Snapshot s) {
        super(s.reportId()); experienceId=s.experienceId(); coffeeId=s.coffeeId(); authorId=s.authorId();
        reporterId=s.reporterId(); reason=s.reason(); details=s.details(); status=s.status();
        createdAt=s.createdAt(); resolvedAt=s.resolvedAt(); version=s.version();
    }
    public static ExperienceReport create(UUID reportId, UUID experienceId, UUID coffeeId,
                                          UUID authorId, UUID reporterId, ExperienceReportReason reason,
                                          String details, Instant now) {
        if (authorId.equals(reporterId)) throw new BusinessCommandRejectedException(
                "EXPERIENCE_SELF_REPORT", "Own experience cannot be reported");
        String normalized = details == null || details.isBlank() ? null : details.strip();
        if (normalized != null && normalized.length() > 1000) throw new BusinessCommandRejectedException(
                "REPORT_DETAILS_TOO_LONG", "Report details exceed 1000 characters");
        return new ExperienceReport(new Snapshot(reportId, experienceId, coffeeId, authorId,
                reporterId, reason, normalized, ExperienceReportStatus.OPEN, now, null, 0));
    }
    public static ExperienceReport fromSnapshot(Snapshot snapshot) { return new ExperienceReport(snapshot); }
    public boolean resolve(ExperienceReportStatus decision, Instant now) {
        if (decision == ExperienceReportStatus.OPEN) throw new IllegalArgumentException("Decision must close report");
        if (status == decision) return false; status=decision; resolvedAt=now; version++; return true;
    }
    public void registerCreated(UUID commandId, Instant clientAt, Instant now) {
        registerEvent(new ExperienceReportedEvent(UUID.randomUUID(), commandId, id, experienceId,
                coffeeId, authorId, reporterId, reason, details, status, version, now, clientAt));
    }
    public Snapshot toSnapshot() { return new Snapshot(id,experienceId,coffeeId,authorId,reporterId,
            reason,details,status,createdAt,resolvedAt,version); }
    public record Snapshot(UUID reportId, UUID experienceId, UUID coffeeId, UUID authorId,
                           UUID reporterId, ExperienceReportReason reason, String details,
                           ExperienceReportStatus status, Instant createdAt, Instant resolvedAt,
                           long version) { }
}
