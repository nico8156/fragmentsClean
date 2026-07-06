package com.nm.fragmentsclean.platform.eventing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;

import com.nm.fragmentsclean.sharedKernel.adapters.secondary.gateways.repositories.jpa.entities.OutboxEventJpaEntity;

public class IntegrationEventEnvelopeFactory {

    private final IntegrationEventPayloadMapper payloadMapper;

    public IntegrationEventEnvelopeFactory() {
        this(new ObjectMapper().findAndRegisterModules());
    }

    public IntegrationEventEnvelopeFactory(ObjectMapper objectMapper) {
        this.payloadMapper = new IntegrationEventPayloadMapper(objectMapper);
    }

    public IntegrationEventEnvelope from(OutboxEventJpaEntity event, String destination) {
        String stableType = IntegrationEventTypeCatalog.stableTypeForClassName(event.getEventType());
        String publicPayloadJson = payloadMapper.toPublicPayloadJson(stableType, event);
        return new IntegrationEventEnvelope(
                event.getEventId(),
                stableType,
                IntegrationEventTypeCatalog.currentVersion(stableType),
                event.getEventType(),
                event.getAggregateType(),
                event.getAggregateId(),
                event.getStreamKey(),
                destination,
                publicPayloadJson,
                event.getOccurredAt()
        );
    }
}
