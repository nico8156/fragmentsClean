package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases;
import java.util.UUID;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.AuthenticatedCommand;
public record ChangeAvatarMediaLifecycleCommand(UUID commandId, UUID mediaId, UUID operatorId,
    String status, String reason) implements AuthenticatedCommand {
  public UUID receiptCommandId(){return commandId;}
  public UUID requesterId(){return operatorId;}
  public String receiptType(){return "user.avatar.admin.lifecycle.v1";}
}
