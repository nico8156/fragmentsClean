package com.nm.fragmentsclean.ticketContext.write.adapters.primary.springboot.sqs;

import static com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinations.TICKET_EVENTS;

import com.nm.fragmentsclean.platform.eventing.contracts.AppUserDeletionRequestedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import com.nm.fragmentsclean.ticketContext.write.businesslogic.usecases.EraseTicketAccountData;
import org.springframework.stereotype.Component;

@Component
public final class AppUserDeletionRequestedTicketSqsHandler implements SqsIntegrationEventHandler {
  private final EraseTicketAccountData handler;
  private final SqsIntegrationEventPayloadReader reader;

  public AppUserDeletionRequestedTicketSqsHandler(
      EraseTicketAccountData handler, SqsIntegrationEventPayloadReader reader) {
    this.handler = handler;
    this.reader = reader;
  }

  @Override
  public SqsIntegrationEventRoute route() {
    return new SqsIntegrationEventRoute(TICKET_EVENTS, "app.user.deletion_requested");
  }

  @Override
  public void handle(IntegrationEventEnvelope envelope) {
    handler.handle(reader.read(envelope, AppUserDeletionRequestedIntegrationEvent.class));
  }
}
