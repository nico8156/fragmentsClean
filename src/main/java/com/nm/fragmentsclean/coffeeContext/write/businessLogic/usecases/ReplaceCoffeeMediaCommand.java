package com.nm.fragmentsclean.coffeeContext.write.businessLogic.usecases;
import java.util.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.AuthenticatedCommand;
public record ReplaceCoffeeMediaCommand(UUID commandId,UUID coffeeId,UUID mediaId,UUID operatorId,String fileName,String contentType,byte[] bytes,String reason) implements AuthenticatedCommand {
 public ReplaceCoffeeMediaCommand {Objects.requireNonNull(commandId);Objects.requireNonNull(coffeeId);Objects.requireNonNull(mediaId);Objects.requireNonNull(operatorId);fileName=fileName==null||fileName.isBlank()?"admin-upload":fileName.strip();bytes=bytes==null?new byte[0]:bytes.clone();}
 public byte[] bytes(){return bytes.clone();}
 public UUID receiptCommandId(){return commandId;}public UUID requesterId(){return operatorId;}public String receiptType(){return "coffee.photo.admin.replace.v1";}
}
