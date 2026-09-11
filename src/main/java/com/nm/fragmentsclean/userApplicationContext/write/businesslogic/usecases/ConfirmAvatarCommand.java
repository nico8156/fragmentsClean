package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.AuthenticatedCommand;import java.time.Instant;import java.util.UUID;
public record ConfirmAvatarCommand(UUID commandId,UUID mediaId,UUID userId,String objectKey,String contentType,long size,int width,int height,String sha256,Instant clientAt)implements AuthenticatedCommand{public UUID receiptCommandId(){return commandId;}public UUID requesterId(){return userId;}public String receiptType(){return "user.avatar.confirm.v1";}}
