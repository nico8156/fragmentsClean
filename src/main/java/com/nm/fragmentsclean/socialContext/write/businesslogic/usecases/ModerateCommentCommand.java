package com.nm.fragmentsclean.socialContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.AuthenticatedCommand;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.ModerationStatus;
import java.time.Instant;
import java.util.UUID;

public record ModerateCommentCommand(UUID commandId, UUID actionId, UUID reportId, UUID commentId,
                                     UUID operatorId, ModerationStatus moderation, String reason,
                                     Instant clientAt) implements AuthenticatedCommand {
    @Override public UUID receiptCommandId() { return commandId; }
    @Override public UUID requesterId() { return operatorId; }
    @Override public String receiptType() { return "social.comment.moderate.v1"; }
}
