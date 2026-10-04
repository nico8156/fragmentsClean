package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.*;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

@Component @Transactional
public class ChangeAvatarMediaLifecycleCommandHandler implements CommandHandler<ChangeAvatarMediaLifecycleCommand> {
  private final AppUserRepository users;
  private final AvatarMediaRepository media;
  private final DomainEventPublisher events;
  private final AdminAuditRecorder audit;
  private final DateTimeProvider clock;
  public ChangeAvatarMediaLifecycleCommandHandler(AppUserRepository users, AvatarMediaRepository media,
      DomainEventPublisher events, AdminAuditRecorder audit, DateTimeProvider clock) {
    this.users=users; this.media=media; this.events=events; this.audit=audit; this.clock=clock;
  }
  public void execute(ChangeAvatarMediaLifecycleCommand command) {
    if(command.reason()==null || command.reason().isBlank() || command.reason().strip().length()>240)
      throw new BusinessCommandRejectedException("MEDIA_REASON_REQUIRED", "A reason of 1 to 240 characters is required");
    var snapshot=media.inspect(command.mediaId()).orElseThrow(()->missing());
    if(snapshot.userId()==null) throw missing();
    // Match all existing avatar writers: lock owner first, then tracked media.
    var user=users.findById(snapshot.userId()).orElseThrow(()->missing());
    if(user.lifecycleStatus()!=AppUserLifecycleStatus.ACTIVE)
      throw new BusinessCommandRejectedException("ACCOUNT_NOT_ACTIVE", "Avatar owner is not active");
    var item=media.byId(command.mediaId()).orElseThrow(()->missing());
    if(!Objects.equals(item.snapshot().userId(),user.id())) throw missing();
    if(!"RETIRED".equals(command.status()) && !"AVAILABLE".equals(command.status()) && !"PURGE_REQUESTED".equals(command.status()))
      throw new BusinessCommandRejectedException("MEDIA_STATUS_INVALID", "Unsupported avatar lifecycle decision");
    var now=clock.now();
    boolean used=media.profileUsageCount(item.snapshot().objectKey())>0;
    boolean retired="RETIRED".equals(command.status());
    boolean purging="PURGE_REQUESTED".equals(command.status());
    boolean changed=purging ? item.requestAdminPurge(used,now) : retired ? item.retire(used,now) : item.restore(used,
        media.activeByUser(user.id()).filter(other->!other.id().equals(item.id())).isPresent(),now);
    if(changed) {
      media.save(item);
      events.publish(AvatarMediaChangedEvent.from(item.snapshot(),command.commandId(),null));
    }
    audit.recordDecision(command.operatorId(),purging?"AVATAR_MEDIA_PURGE_REQUESTED":retired?"AVATAR_MEDIA_RETIRED":"AVATAR_MEDIA_RESTORED","AVATAR_MEDIA",item.id(),
        command.commandId(),"APPLIED",command.reason().strip(),now);
  }
  private static BusinessCommandRejectedException missing(){return new BusinessCommandRejectedException("MEDIA_NOT_TRACKED","Tracked avatar with active ownership not found");}
}
