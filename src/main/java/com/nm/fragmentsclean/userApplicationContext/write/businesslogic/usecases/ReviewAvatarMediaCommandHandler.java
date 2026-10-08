package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.*;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.media.PrivateMediaReferences;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

@Component @Transactional
public class ReviewAvatarMediaCommandHandler implements CommandHandler<ReviewAvatarMediaCommand> {
  private final AppUserRepository users;private final AvatarMediaRepository media;private final DomainEventPublisher events;private final AdminAuditRecorder audit;private final DateTimeProvider clock;
  public ReviewAvatarMediaCommandHandler(AppUserRepository users,AvatarMediaRepository media,DomainEventPublisher events,AdminAuditRecorder audit,DateTimeProvider clock){this.users=users;this.media=media;this.events=events;this.audit=audit;this.clock=clock;}
  public void execute(ReviewAvatarMediaCommand c){
    if(c.reason()==null||c.reason().isBlank()||c.reason().strip().length()>240)throw new BusinessCommandRejectedException("MEDIA_REASON_REQUIRED","A reason of 1 to 240 characters is required");
    var snapshot=media.inspect(c.mediaId()).orElseThrow(()->missing());
    if(snapshot.userId()==null)throw missing();
    var user=users.findById(snapshot.userId()).orElseThrow(()->missing());
    if(user.lifecycleStatus()!=AppUserLifecycleStatus.ACTIVE)throw new BusinessCommandRejectedException("ACCOUNT_NOT_ACTIVE","Account is not active");
    var item=media.byId(c.mediaId()).orElseThrow(()->missing());
    if(!Objects.equals(item.snapshot().userId(),user.id()))throw missing();
    var previous=media.activeByUser(user.id()).filter(active->!active.id().equals(item.id()));
    if(c.approve()&&previous.isPresent()&&previous.get().snapshot().createdAt().isAfter(item.snapshot().createdAt()))
      throw new BusinessCommandRejectedException("AVATAR_REVIEW_SUPERSEDED","A newer avatar is already active");
    var now=clock.now();
    if(item.review(c.approve(),now)){
      if(c.approve()&&previous.isPresent()){
        var old=previous.get();old.requestDeletion(now);media.replaceActive(old,item);
        events.publish(AvatarMediaChangedEvent.from(old.snapshot(),c.commandId(),null));
      }else media.save(item);
      if(c.approve()&&user.replaceAvatar(PrivateMediaReferences.avatar(item.snapshot().objectKey()),now)){
        users.save(user);user.domainEvents().forEach(events::publish);user.clearDomainEvents();
      }
      events.publish(AvatarMediaChangedEvent.from(item.snapshot(),c.commandId(),c.approve()?user.id():null));
    }
    audit.recordDecision(c.operatorId(),c.approve()?"AVATAR_MEDIA_APPROVED":"AVATAR_MEDIA_REJECTED","AVATAR_MEDIA",item.id(),c.commandId(),"APPLIED",c.reason().strip(),now);
  }
  private static BusinessCommandRejectedException missing(){return new BusinessCommandRejectedException("MEDIA_NOT_FOUND","Image not found");}
}
