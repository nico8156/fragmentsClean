package com.nm.fragmentsclean.ticketContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.platform.eventing.contracts.AppUserDeletionRequestedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketAccountDataEraser;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketAccountDataErasedEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.privacy.AccountErasureBarrier;
import java.util.UUID;

public class EraseTicketAccountData {
  private final TicketAccountDataEraser eraser;
  private final DomainEventPublisher events;
  private final DateTimeProvider clock;
  private final AccountErasureBarrier barrier;

  public EraseTicketAccountData(
      TicketAccountDataEraser eraser, DomainEventPublisher events, DateTimeProvider clock,
      AccountErasureBarrier barrier) {
    this.eraser = eraser;
    this.events = events;
    this.clock = clock;
    this.barrier = barrier;
  }

  public void handle(AppUserDeletionRequestedIntegrationEvent request) {
    var now = clock.now();
    barrier.erase(AccountErasureBarrier.Scope.TICKET, request.userId(), request.requestId(), now, () -> {
      eraser.erase(request.userId());
      events.publish(
          new TicketAccountDataErasedEvent(
              UUID.randomUUID(), request.requestId(), request.userId(), "TICKET", now));
    });
  }
}
