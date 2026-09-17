package com.nm.fragmentsclean.socialContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.platform.eventing.contracts.AppUserDeletionRequestedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.SocialAccountDataEraser;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.SocialAccountDataErasedEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.privacy.AccountErasureBarrier;
import java.util.UUID;

public class EraseSocialAccountData {
  private final SocialAccountDataEraser eraser;
  private final DomainEventPublisher events;
  private final DateTimeProvider clock;
  private final AccountErasureBarrier barrier;

  public EraseSocialAccountData(
      SocialAccountDataEraser eraser, DomainEventPublisher events, DateTimeProvider clock,
      AccountErasureBarrier barrier) {
    this.eraser = eraser;
    this.events = events;
    this.clock = clock;
    this.barrier = barrier;
  }

  public void handle(AppUserDeletionRequestedIntegrationEvent request) {
    var now = clock.now();
    barrier.erase(AccountErasureBarrier.Scope.SOCIAL, request.userId(), request.requestId(), now, () -> {
      eraser.erase(request.userId());
      events.publish(
          new SocialAccountDataErasedEvent(
              UUID.randomUUID(), request.requestId(), request.userId(), "SOCIAL", now));
    });
  }
}
