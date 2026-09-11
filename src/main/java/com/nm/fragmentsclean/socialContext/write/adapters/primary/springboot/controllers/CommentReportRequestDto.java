package com.nm.fragmentsclean.socialContext.write.adapters.primary.springboot.controllers;

import com.nm.fragmentsclean.socialContext.write.businesslogic.models.ReportReason;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CommentReportRequestDto(UUID commandId, UUID reportId, ReportReason reason, String details, Instant at) {
    public CommentReportRequestDto {
        Objects.requireNonNull(commandId); Objects.requireNonNull(reportId);
        Objects.requireNonNull(reason); Objects.requireNonNull(at);
    }
}
