package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.AuthenticatedCommand;
import java.util.UUID;

public record UpdateAppUserProfileCommand(UUID commandId, UUID userId, String displayName)
    implements AuthenticatedCommand {
  @Override
  public UUID receiptCommandId() {
    return commandId;
  }

  @Override
  public UUID requesterId() {
    return userId;
  }

  @Override
  public String receiptType() {
    return "app.user.profile.update.v1";
  }
}
