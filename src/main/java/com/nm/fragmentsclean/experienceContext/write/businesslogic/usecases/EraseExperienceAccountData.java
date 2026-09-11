package com.nm.fragmentsclean.experienceContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.experienceContext.write.businesslogic.gateways.ExperienceAccountDataEraser;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperienceAccountDataErasedEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.AppUserDeletionRequestedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.*;
import jakarta.transaction.Transactional;
import java.util.UUID;

@Transactional
public class EraseExperienceAccountData{
    private final ExperienceAccountDataEraser eraser;private final DomainEventPublisher events;private final DateTimeProvider clock;
    public EraseExperienceAccountData(ExperienceAccountDataEraser eraser,DomainEventPublisher events,DateTimeProvider clock){this.eraser=eraser;this.events=events;this.clock=clock;}
    public void handle(AppUserDeletionRequestedIntegrationEvent request){eraser.erase(request.userId());var now=clock.now();events.publish(new ExperienceAccountDataErasedEvent(UUID.randomUUID(),request.requestId(),request.userId(),"EXPERIENCE",now));}
}
