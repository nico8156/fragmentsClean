package com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.providers.outboxEventPublisher;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;

import java.util.ArrayList;
import java.util.List;

public class FakeDomainEventPublisher implements DomainEventPublisher {
    public List<DomainEvent> published = new ArrayList<>();

    @Override
    public void publish(DomainEvent event) {
        published.add(event);
    }
}
