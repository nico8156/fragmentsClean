package com.nm.fragmentsclean.sharedKernel.businesslogic.eventing;

import java.time.Instant;

public record IntegrationEventEnvelope(
        String eventId,
        String eventType,
        int eventVersion,
        String sourceEventClassName,
        String aggregateType,
        String aggregateId,
        String streamKey,
        String destination,
        String payloadJson,
        Instant occurredAt
) {
}
