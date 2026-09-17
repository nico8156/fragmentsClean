package com.nm.fragmentsclean.experienceContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.experienceContext.write.businesslogic.gateways.ExperienceAccountDataEraser;
import com.nm.fragmentsclean.experienceContext.write.businesslogic.models.ExperienceAccountDataErasedEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.AppUserDeletionRequestedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.privacy.AccountErasureBarrier;
import java.util.UUID;

public class EraseExperienceAccountData{
    private final ExperienceAccountDataEraser eraser;private final DomainEventPublisher events;private final DateTimeProvider clock;private final AccountErasureBarrier barrier;
    public EraseExperienceAccountData(ExperienceAccountDataEraser eraser,DomainEventPublisher events,DateTimeProvider clock,AccountErasureBarrier barrier){this.eraser=eraser;this.events=events;this.clock=clock;this.barrier=barrier;}
    public void handle(AppUserDeletionRequestedIntegrationEvent request){var now=clock.now();barrier.erase(AccountErasureBarrier.Scope.EXPERIENCE,request.userId(),request.requestId(),now,()->{eraser.erase(request.userId());events.publish(new ExperienceAccountDataErasedEvent(UUID.randomUUID(),request.requestId(),request.userId(),"EXPERIENCE",now));});}
}
