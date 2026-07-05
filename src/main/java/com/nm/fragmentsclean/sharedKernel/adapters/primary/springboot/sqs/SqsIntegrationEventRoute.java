package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs;

import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;

public record SqsIntegrationEventRoute(String destination, String eventType) {

    public boolean matches(IntegrationEventEnvelope envelope) {
        return destination.equals(envelope.destination()) && eventType.equals(envelope.eventType());
    }
}
