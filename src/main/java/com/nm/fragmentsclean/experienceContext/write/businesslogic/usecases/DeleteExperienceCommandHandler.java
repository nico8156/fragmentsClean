package com.nm.fragmentsclean.experienceContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.experienceContext.write.businesslogic.gateways.ExperienceMediaRepository;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.gateways.ExperienceRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import jakarta.transaction.Transactional;

@Transactional
public class DeleteExperienceCommandHandler implements CommandHandler<DeleteExperienceCommand>{
    private final ExperienceRepository experiences;private final ExperienceMediaRepository media;private final DomainEventPublisher events;private final DateTimeProvider clock;
    public DeleteExperienceCommandHandler(ExperienceRepository experiences,ExperienceMediaRepository media,DomainEventPublisher events,DateTimeProvider clock){this.experiences=experiences;this.media=media;this.events=events;this.clock=clock;}
    @Override public void execute(DeleteExperienceCommand command){var experience=experiences.byId(command.experienceId()).orElseThrow(()->new BusinessCommandRejectedException("EXPERIENCE_NOT_FOUND","Experience does not exist"));var now=clock.now();if(!experience.delete(command.userId(),now))return;for(var item:media.byExperience(command.experienceId())){if(item.requestDeletion(command.userId(),now)){media.save(item);item.registerChanged(command.commandId(),"EXPERIENCE_DELETED",command.clientAt(),now);ExperienceEvents.publish(item,events);}}experiences.save(experience);experience.registerChange(command.commandId(),"DELETED",command.clientAt(),now);ExperienceEvents.publish(experience,events);}
}
