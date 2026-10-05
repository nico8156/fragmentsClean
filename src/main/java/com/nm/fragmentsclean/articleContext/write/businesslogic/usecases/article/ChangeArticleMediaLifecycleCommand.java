package com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article;
import java.util.UUID;import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.AuthenticatedCommand;
public record ChangeArticleMediaLifecycleCommand(UUID commandId,UUID mediaId,UUID operatorId,String status,String reason) implements AuthenticatedCommand {
 public UUID receiptCommandId(){return commandId;}public UUID requesterId(){return operatorId;}public String receiptType(){return "article.media.lifecycle.v1";}
}
