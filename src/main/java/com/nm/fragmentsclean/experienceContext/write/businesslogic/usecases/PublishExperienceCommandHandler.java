package com.nm.fragmentsclean.experienceContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.experienceContext.write.businesslogic.gateways.ExperienceMediaRepository;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.gateways.ExperienceRepository;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperienceContentPolicy;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import jakarta.transaction.Transactional;

@Transactional
public class PublishExperienceCommandHandler implements CommandHandler<PublishExperienceCommand>{
    private final ExperienceRepository experiences;private final ExperienceMediaRepository media;private final ExperienceContentPolicy policy;private final DomainEventPublisher events;private final DateTimeProvider clock;
    public PublishExperienceCommandHandler(ExperienceRepository experiences,ExperienceMediaRepository media,ExperienceContentPolicy policy,DomainEventPublisher events,DateTimeProvider clock){this.experiences=experiences;this.media=media;this.policy=policy;this.events=events;this.clock=clock;}
    @Override public void execute(PublishExperienceCommand command){var experience=experiences.byId(command.experienceId()).orElseThrow(()->new BusinessCommandRejectedException("EXPERIENCE_NOT_FOUND","Experience does not exist"));if(media.hasPending(command.experienceId()))throw new BusinessCommandRejectedException("EXPERIENCE_MEDIA_PENDING","Experience cannot be published while a media upload is pending");var now=clock.now();if(!experience.publish(command.userId(),policy,now))return;experiences.save(experience);experience.registerChange(command.commandId(),"PUBLISHED",command.clientAt(),now);ExperienceEvents.publish(experience,events);}
}
