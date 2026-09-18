package com.nm.fragmentsclean.sharedKernel.businesslogic.eventing;

import java.time.Instant;

/**
 * Provider-neutral representation of a durable message ready for delivery.
 * Persistence entities must not cross the outbox port boundary.
 */
public record OutboxMessage(
        long id,
        String eventId,
        String eventType,
        String aggregateType,
        String aggregateId,
        String streamKey,
        String payloadJson,
        Instant occurredAt,
        Instant createdAt,
        int retryCount
) implements OutboxEventData {
    @Override public Long getId() { return id; }
    @Override public String getEventId() { return eventId; }
    @Override public String getEventType() { return eventType; }
    @Override public String getAggregateType() { return aggregateType; }
    @Override public String getAggregateId() { return aggregateId; }
    @Override public String getStreamKey() { return streamKey; }
    @Override public String getPayloadJson() { return payloadJson; }
    @Override public Instant getOccurredAt() { return occurredAt; }
    @Override public Instant getCreatedAt() { return createdAt; }
    @Override public Integer getRetryCount() { return retryCount; }
}
