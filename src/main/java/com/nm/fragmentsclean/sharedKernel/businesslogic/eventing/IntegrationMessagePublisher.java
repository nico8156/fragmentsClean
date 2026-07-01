package com.nm.fragmentsclean.sharedKernel.businesslogic.eventing;

public interface IntegrationMessagePublisher {
    void publish(IntegrationEventEnvelope envelope) throws Exception;
}
