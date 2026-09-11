package com.nm.fragmentsclean.socialContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.platform.eventing.contracts.AppUserDeletionRequestedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.SocialAccountDataEraser;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.SocialAccountDataErasedEvent;
import jakarta.transaction.Transactional;
import java.util.UUID;

@Transactional
public class EraseSocialAccountData {
  private final SocialAccountDataEraser eraser;
  private final DomainEventPublisher events;
  private final DateTimeProvider clock;

  public EraseSocialAccountData(
      SocialAccountDataEraser eraser, DomainEventPublisher events, DateTimeProvider clock) {
    this.eraser = eraser;
    this.events = events;
    this.clock = clock;
  }

  public void handle(AppUserDeletionRequestedIntegrationEvent request) {
    eraser.erase(request.userId());
    var now = clock.now();
    events.publish(
        new SocialAccountDataErasedEvent(
            UUID.randomUUID(), request.requestId(), request.userId(), "SOCIAL", now));
  }
}
