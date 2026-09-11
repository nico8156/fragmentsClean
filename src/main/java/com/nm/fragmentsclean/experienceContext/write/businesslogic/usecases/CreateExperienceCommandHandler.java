package com.nm.fragmentsclean.experienceContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.experienceContext.write.businesslogic.gateways.*;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus.BusinessCommandRejectedException;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.command.CommandHandler;
import jakarta.transaction.Transactional;
import java.util.Objects;

@Transactional
public class CreateExperienceCommandHandler implements CommandHandler<CreateExperienceCommand> {
    private final ExperienceRepository experiences; private final ExperienceCoffeeReference coffees;
    private final ExperienceContentPolicy policy; private final DomainEventPublisher events;
    private final DateTimeProvider clock;
    public CreateExperienceCommandHandler(ExperienceRepository experiences, ExperienceCoffeeReference coffees,
                                          ExperienceContentPolicy policy, DomainEventPublisher events,
                                          DateTimeProvider clock) {
        this.experiences=experiences;this.coffees=coffees;this.policy=policy;this.events=events;this.clock=clock;
    }
    @Override public void execute(CreateExperienceCommand command) {
        var existing=experiences.byId(command.experienceId());
        if(existing.isPresent()) {
            var s=existing.get().toSnapshot();
            String normalized=command.publicationStatus()==ExperiencePublicationStatus.PUBLISHED
                    ? policy.requirePublishable(command.message()):policy.normalizeDraft(command.message());
            if(!s.userId().equals(command.userId())||!s.coffeeId().equals(command.coffeeId())
                    ||!Objects.equals(s.message(),normalized)||s.publicationStatus()!=command.publicationStatus())
                throw new BusinessCommandRejectedException("EXPERIENCE_ID_CONFLICT","Experience id belongs to another intent");
            return;
        }
        if(!coffees.isAvailable(command.coffeeId())) throw new BusinessCommandRejectedException(
                "COFFEE_NOT_AVAILABLE","Coffee is not available for an experience");
        var now=clock.now();
        var experience=Experience.create(command.experienceId(),command.userId(),command.coffeeId(),
                command.message(),command.publicationStatus(),policy,now);
        experiences.save(experience); experience.registerChange(command.commandId(),"CREATED",command.clientAt(),now);
        ExperienceEvents.publish(experience,events);
    }
}
