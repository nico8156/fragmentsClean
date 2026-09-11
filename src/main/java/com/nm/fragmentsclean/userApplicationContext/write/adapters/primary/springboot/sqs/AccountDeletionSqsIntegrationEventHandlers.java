package com.nm.fragmentsclean.userApplicationContext.write.adapters.primary.springboot.sqs;

import static com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinations.APP_USERS_EVENTS;

import com.nm.fragmentsclean.platform.eventing.contracts.AccountDataErasedIntegrationEvent;
import com.nm.fragmentsclean.platform.eventing.contracts.AppUserDeletionRequestedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.*;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.processManagers.AccountDeletionProcessManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AccountDeletionSqsIntegrationEventHandlers {
  private final SqsIntegrationEventPayloadReader reader;

  public AccountDeletionSqsIntegrationEventHandlers(SqsIntegrationEventPayloadReader reader) {
    this.reader = reader;
  }

  @Bean
  SqsIntegrationEventHandler appUserDeletionLocalDataHandler(
      AccountDeletionProcessManager manager) {
    return simple(
        "app.user.deletion_requested",
        e ->
            manager.eraseLocalData(reader.read(e, AppUserDeletionRequestedIntegrationEvent.class)));
  }

  @Bean
  SqsIntegrationEventHandler accountDataErasedHandler(AccountDeletionProcessManager manager) {
    return simple(
        "account.data_erased",
        e -> manager.acknowledge(reader.read(e, AccountDataErasedIntegrationEvent.class)));
  }

  private SqsIntegrationEventHandler simple(
      String type, java.util.function.Consumer<IntegrationEventEnvelope> consumer) {
    return new SqsIntegrationEventHandler() {
      @Override
      public SqsIntegrationEventRoute route() {
        return new SqsIntegrationEventRoute(APP_USERS_EVENTS, type);
      }

      @Override
      public void handle(IntegrationEventEnvelope envelope) {
        consumer.accept(envelope);
      }
    };
  }
}
