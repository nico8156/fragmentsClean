package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs;

import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;

interface SqsIntegrationEventRouting {
    void route(IntegrationEventEnvelope envelope);
}
