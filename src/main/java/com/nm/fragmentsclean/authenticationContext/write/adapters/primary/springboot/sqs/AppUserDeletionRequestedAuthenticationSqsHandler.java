package com.nm.fragmentsclean.authenticationContext.write.adapters.primary.springboot.sqs;

import static com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinations.AUTH_USERS_EVENTS;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases.EraseAuthenticationAccountData;
import com.nm.fragmentsclean.platform.eventing.contracts.AppUserDeletionRequestedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import org.springframework.stereotype.Component;

@Component
public final class AppUserDeletionRequestedAuthenticationSqsHandler
    implements SqsIntegrationEventHandler {
  private final EraseAuthenticationAccountData handler;
  private final SqsIntegrationEventPayloadReader reader;

  public AppUserDeletionRequestedAuthenticationSqsHandler(
      EraseAuthenticationAccountData handler, SqsIntegrationEventPayloadReader reader) {
    this.handler = handler;
    this.reader = reader;
  }

  @Override
  public SqsIntegrationEventRoute route() {
    return new SqsIntegrationEventRoute(AUTH_USERS_EVENTS, "app.user.deletion_requested");
  }

  @Override
  public void handle(IntegrationEventEnvelope envelope) {
    handler.handle(reader.read(envelope, AppUserDeletionRequestedIntegrationEvent.class));
  }
}
