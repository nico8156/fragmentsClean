package com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.entities;

import com.nm.fragmentsclean.socialContext.write.businesslogic.models.ReportReason;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.ReportStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "content_reports")
@Getter @NoArgsConstructor
public class ContentReportJpaEntity {
    @Id private UUID reportId;
    private UUID commentId;
    private UUID targetId;
    private UUID authorId;
    private UUID reporterId;
    @Enumerated(EnumType.STRING) private ReportReason reason;
    private String details;
    @Enumerated(EnumType.STRING) private ReportStatus status;
    private Instant createdAt;
    private Instant resolvedAt;
    private long version;

    public ContentReportJpaEntity(UUID reportId, UUID commentId, UUID targetId, UUID authorId,
                                  UUID reporterId, ReportReason reason, String details, ReportStatus status,
                                  Instant createdAt, Instant resolvedAt, long version) {
        this.reportId = reportId; this.commentId = commentId; this.targetId = targetId;
        this.authorId = authorId; this.reporterId = reporterId; this.reason = reason;
        this.details = details; this.status = status; this.createdAt = createdAt;
        this.resolvedAt = resolvedAt; this.version = version;
    }
}
