package com.nm.fragmentsclean.experienceContext.write.businesslogic.usecases;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.gateways.*;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperiencePublicationStatus;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component @Transactional
public class ReviewExperienceMediaCommandHandler implements CommandHandler<ReviewExperienceMediaCommand> {
  private final ExperienceRepository experiences;
  private final ExperienceMediaRepository media;
  private final DomainEventPublisher events;
  private final AdminAuditRecorder audit;
  private final DateTimeProvider clock;
  public ReviewExperienceMediaCommandHandler(ExperienceRepository experiences,ExperienceMediaRepository media,DomainEventPublisher events,AdminAuditRecorder audit,DateTimeProvider clock){this.experiences=experiences;this.media=media;this.events=events;this.audit=audit;this.clock=clock;}
  public void execute(ReviewExperienceMediaCommand c){
    if(c.reason()==null||c.reason().isBlank()||c.reason().strip().length()>240)throw new BusinessCommandRejectedException("MEDIA_REASON_REQUIRED","A reason of 1 to 240 characters is required");
    var snapshot=media.inspect(c.mediaId()).orElseThrow(()->new BusinessCommandRejectedException("MEDIA_NOT_FOUND","Image not found"));
    var experience=experiences.byId(snapshot.experienceId()).orElseThrow(()->new BusinessCommandRejectedException("EXPERIENCE_NOT_FOUND","Experience not found"));
    if(experience.toSnapshot().publicationStatus()==ExperiencePublicationStatus.DELETED||snapshot.userId()==null)throw new BusinessCommandRejectedException("EXPERIENCE_DELETED","Deleted experience cannot publish an image");
    var item=media.byId(c.mediaId()).orElseThrow();
    var now=clock.now();
    if(item.review(c.approve(),now)){
      media.save(item);item.registerChanged(c.commandId(),c.approve()?"MODERATION_APPROVED":"MODERATION_REJECTED",null,now);ExperienceEvents.publish(item,events);
    }
    audit.recordDecision(c.operatorId(),c.approve()?"EXPERIENCE_MEDIA_APPROVED":"EXPERIENCE_MEDIA_REJECTED","EXPERIENCE_MEDIA",item.id(),c.commandId(),"APPLIED",c.reason().strip(),now);
  }
}
