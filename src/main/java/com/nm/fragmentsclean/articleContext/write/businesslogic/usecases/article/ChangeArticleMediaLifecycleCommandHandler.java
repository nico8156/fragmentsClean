package com.nm.fragmentsclean.articleContext.write.businesslogic.usecases.article;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import org.springframework.stereotype.Component;import org.springframework.transaction.annotation.Transactional;
@Component @Transactional public class ChangeArticleMediaLifecycleCommandHandler implements CommandHandler<ChangeArticleMediaLifecycleCommand> {
 private final ArticleMediaLifecycleRepository repository;private final ArticleMediaCatalogPublisher catalogue;private final AdminAuditRecorder audit;private final DateTimeProvider clock;
 public ChangeArticleMediaLifecycleCommandHandler(ArticleMediaLifecycleRepository repository,ArticleMediaCatalogPublisher catalogue,AdminAuditRecorder audit,DateTimeProvider clock){this.repository=repository;this.catalogue=catalogue;this.audit=audit;this.clock=clock;}
 public void execute(ChangeArticleMediaLifecycleCommand command){
  if(command.reason()==null||command.reason().isBlank()||command.reason().strip().length()>240)throw new BusinessCommandRejectedException("MEDIA_REASON_REQUIRED","A reason of 1 to 240 characters is required");
  var media=repository.lock(command.mediaId()).orElseThrow(()->new BusinessCommandRejectedException("MEDIA_NOT_TRACKED","Tracked article upload not found"));
  String status=media.transition(command.status());var at=clock.now();
  if(!status.equals(media.status())){repository.change(media.mediaId(),status,at);catalogue.publish(media.articleId());}
  audit.recordDecision(command.operatorId(),"ARTICLE_MEDIA_"+(status.equals("RETIRED")?"RETIRED":"RESTORED"),"ARTICLE_MEDIA",media.mediaId(),command.commandId(),"APPLIED",command.reason().strip(),at);
 }
}
