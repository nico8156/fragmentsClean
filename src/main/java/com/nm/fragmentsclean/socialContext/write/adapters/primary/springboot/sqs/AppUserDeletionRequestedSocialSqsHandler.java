package com.nm.fragmentsclean.socialContext.write.adapters.primary.springboot.sqs;

import static com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinations.DOMAIN_EVENTS;

import com.nm.fragmentsclean.platform.eventing.contracts.AppUserDeletionRequestedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import com.nm.fragmentsclean.socialContext.write.businesslogic.usecases.EraseSocialAccountData;
import org.springframework.stereotype.Component;

@Component
public final class AppUserDeletionRequestedSocialSqsHandler implements SqsIntegrationEventHandler {
  private final EraseSocialAccountData handler;
  private final SqsIntegrationEventPayloadReader reader;

  public AppUserDeletionRequestedSocialSqsHandler(
      EraseSocialAccountData handler, SqsIntegrationEventPayloadReader reader) {
    this.handler = handler;
    this.reader = reader;
  }

  @Override
  public SqsIntegrationEventRoute route() {
    return new SqsIntegrationEventRoute(DOMAIN_EVENTS, "app.user.deletion_requested");
  }

  @Override
  public void handle(IntegrationEventEnvelope envelope) {
    handler.handle(reader.read(envelope, AppUserDeletionRequestedIntegrationEvent.class));
  }
}
