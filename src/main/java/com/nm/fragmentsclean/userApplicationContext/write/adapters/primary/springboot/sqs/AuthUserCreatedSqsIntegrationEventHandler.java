package com.nm.fragmentsclean.userApplicationContext.write.adapters.primary.springboot.sqs;

import static com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinations.AUTH_USERS_EVENTS;

import com.nm.fragmentsclean.platform.eventing.contracts.AuthUserCreatedIntegrationEvent;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventHandler;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventPayloadReader;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventRoute;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases.AuthUserCreatedEventHandler;
import org.springframework.stereotype.Component;
import com.nm.fragmentsclean.sharedKernel.businesslogic.privacy.AccountErasureBarrier;

@Component
public class AuthUserCreatedSqsIntegrationEventHandler implements SqsIntegrationEventHandler {

    private final AuthUserCreatedEventHandler handler;
    private final SqsIntegrationEventPayloadReader payloadReader;
    private final AccountErasureBarrier barrier;

    public AuthUserCreatedSqsIntegrationEventHandler(
            AuthUserCreatedEventHandler handler,
            SqsIntegrationEventPayloadReader payloadReader,
            AccountErasureBarrier barrier) {
        this.handler = handler;
        this.payloadReader = payloadReader;
        this.barrier = barrier;
    }

    @Override
    public SqsIntegrationEventRoute route() {
        return new SqsIntegrationEventRoute(AUTH_USERS_EVENTS, "auth.user.created");
    }

    @Override
    public void handle(IntegrationEventEnvelope envelope) {
        var event=payloadReader.read(envelope, AuthUserCreatedIntegrationEvent.class);
        barrier.ifActive(AccountErasureBarrier.Scope.USER_APPLICATION,event.authUserId(),()->handler.handle(event));
    }
}
