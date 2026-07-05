package com.nm.fragmentsclean.userApplicationContext.write.adapters.primary.springboot.sqs;

import static com.nm.fragmentsclean.platform.eventing.IntegrationEventDestinations.AUTH_USERS_EVENTS;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthUserCreatedEvent;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventHandler;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventPayloadReader;
import com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs.SqsIntegrationEventRoute;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases.AuthUserCreatedEventHandler;
import org.springframework.stereotype.Component;

@Component
public class AuthUserCreatedSqsIntegrationEventHandler implements SqsIntegrationEventHandler {

    private final AuthUserCreatedEventHandler handler;
    private final SqsIntegrationEventPayloadReader payloadReader;

    public AuthUserCreatedSqsIntegrationEventHandler(
            AuthUserCreatedEventHandler handler,
            SqsIntegrationEventPayloadReader payloadReader) {
        this.handler = handler;
        this.payloadReader = payloadReader;
    }

    @Override
    public SqsIntegrationEventRoute route() {
        return new SqsIntegrationEventRoute(AUTH_USERS_EVENTS, "auth.user.created");
    }

    @Override
    public void handle(IntegrationEventEnvelope envelope) {
        handler.handle(payloadReader.read(envelope, AuthUserCreatedEvent.class));
    }
}
