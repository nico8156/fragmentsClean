package com.nm.fragmentsclean.experienceContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.experienceContext.write.businesslogic.gateways.ExperienceRepository;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import jakarta.transaction.Transactional;

@Transactional
public class DeleteExperienceCommandHandler implements CommandHandler<DeleteExperienceCommand>{
    private final ExperienceRepository experiences;private final DomainEventPublisher events;private final DateTimeProvider clock;
    public DeleteExperienceCommandHandler(ExperienceRepository experiences,DomainEventPublisher events,DateTimeProvider clock){this.experiences=experiences;this.events=events;this.clock=clock;}
    @Override public void execute(DeleteExperienceCommand command){var experience=experiences.byId(command.experienceId()).orElseThrow(()->new BusinessCommandRejectedException("EXPERIENCE_NOT_FOUND","Experience does not exist"));var now=clock.now();if(!experience.delete(command.userId(),now))return;experiences.save(experience);experience.registerChange(command.commandId(),"DELETED",command.clientAt(),now);ExperienceEvents.publish(experience,events);}
}
