package com.nm.fragmentsclean.experienceContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperienceReportReason;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.AuthenticatedCommand;
import java.time.Instant;
import java.util.UUID;

public record ReportExperienceCommand(UUID commandId, UUID reportId, UUID experienceId,
                                      UUID reporterId, ExperienceReportReason reason, String details,
                                      Instant clientAt) implements AuthenticatedCommand {
    @Override public UUID receiptCommandId() { return commandId; }
    @Override public UUID requesterId() { return reporterId; }
    @Override public String receiptType() { return "experience.report.v1"; }
}
