package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs;

import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;

public interface SqsIntegrationEventHandler {

    SqsIntegrationEventRoute route();

    void handle(IntegrationEventEnvelope envelope);
}
