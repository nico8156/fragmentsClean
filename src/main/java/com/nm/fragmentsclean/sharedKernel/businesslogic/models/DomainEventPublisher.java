package com.nm.fragmentsclean.sharedKernel.businesslogic.models;

public interface DomainEventPublisher {
    void publish(DomainEvent event);
}
