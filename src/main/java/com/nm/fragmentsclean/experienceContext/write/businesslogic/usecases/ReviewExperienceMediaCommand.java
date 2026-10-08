package com.nm.fragmentsclean.experienceContext.write.businesslogic.usecases;
import java.util.UUID;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.AuthenticatedCommand;
public record ReviewExperienceMediaCommand(UUID commandId,UUID mediaId,UUID operatorId,boolean approve,String reason) implements AuthenticatedCommand {
  public UUID receiptCommandId(){return commandId;}
  public UUID requesterId(){return operatorId;}
  public String receiptType(){return "experience-media.review.v1";}
}
