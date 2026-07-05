package com.nm.fragmentsclean.platform.eventing;

import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;

public class IntegrationEventEnvelopeFactory {

    public IntegrationEventEnvelope from(OutboxEventJpaEntity event, String destination) {
        String stableType = IntegrationEventTypeCatalog.stableTypeForClassName(event.getEventType());
        return new IntegrationEventEnvelope(
                event.getEventId(),
                stableType,
                IntegrationEventTypeCatalog.currentVersion(stableType),
                event.getEventType(),
                event.getAggregateType(),
                event.getAggregateId(),
                event.getStreamKey(),
                destination,
                event.getPayloadJson(),
                event.getOccurredAt()
        );
    }
}
