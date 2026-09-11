package com.nm.fragmentsclean.socialContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.AuthenticatedCommand;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.ReportReason;
import java.time.Instant;
import java.util.UUID;

public record ReportCommentCommand(UUID commandId, UUID reportId, UUID commentId, UUID reporterId,
                                   ReportReason reason, String details, Instant clientAt)
        implements AuthenticatedCommand {
    @Override public UUID receiptCommandId() { return commandId; }
    @Override public UUID requesterId() { return reporterId; }
    @Override public String receiptType() { return "social.comment.report.v1"; }
}
