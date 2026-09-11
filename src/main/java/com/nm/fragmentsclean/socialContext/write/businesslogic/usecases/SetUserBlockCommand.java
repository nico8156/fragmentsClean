package com.nm.fragmentsclean.socialContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.AuthenticatedCommand;
import java.time.Instant;
import java.util.UUID;

public record SetUserBlockCommand(UUID commandId, UUID blockId, UUID blockerId, UUID blockedUserId,
                                  boolean active, Instant clientAt) implements AuthenticatedCommand {
    @Override public UUID receiptCommandId() { return commandId; }
    @Override public UUID requesterId() { return blockerId; }
    @Override public String receiptType() { return active ? "social.user.block.v1" : "social.user.unblock.v1"; }
}
