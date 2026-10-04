package com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases;
import java.util.UUID;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.AuthenticatedCommand;
public record ChangeCoffeeMediaLifecycleCommand(UUID commandId,UUID coffeeId,UUID mediaId,UUID operatorId,String status,String reason) implements AuthenticatedCommand {
    public UUID receiptCommandId(){return commandId;}
    public UUID requesterId(){return operatorId;}
    public String receiptType(){return "coffee.photo.admin.lifecycle.v1";}
}
