package com.nm.fragmentsclean.ticketContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.platform.eventing.contracts.AppUserDeletionRequestedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways.TicketAccountDataEraser;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketAccountDataErasedEvent;
import jakarta.transaction.Transactional;
import java.util.UUID;

@Transactional
public class EraseTicketAccountData {
  private final TicketAccountDataEraser eraser;
  private final DomainEventPublisher events;
  private final DateTimeProvider clock;

  public EraseTicketAccountData(
      TicketAccountDataEraser eraser, DomainEventPublisher events, DateTimeProvider clock) {
    this.eraser = eraser;
    this.events = events;
    this.clock = clock;
  }

  public void handle(AppUserDeletionRequestedIntegrationEvent request) {
    eraser.erase(request.userId());
    var now = clock.now();
    events.publish(
        new TicketAccountDataErasedEvent(
            UUID.randomUUID(), request.requestId(), request.userId(), "TICKET", now));
  }
}
