package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.AuthenticatedCommand;import java.time.Instant;import java.util.UUID;
public record RemoveAvatarCommand(UUID commandId,UUID userId,Instant clientAt)implements AuthenticatedCommand{public UUID receiptCommandId(){return commandId;}public UUID requesterId(){return userId;}public String receiptType(){return "user.avatar.remove.v1";}}
