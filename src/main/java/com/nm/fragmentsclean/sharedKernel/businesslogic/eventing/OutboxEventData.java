package com.nm.fragmentsclean.sharedKernel.businesslogic.eventing;

import java.time.Instant;

/** Read-only event data shared by persistence and delivery adapters. */
public interface OutboxEventData {
    Long getId();
    String getEventId();
    String getEventType();
    String getAggregateType();
    String getAggregateId();
    String getStreamKey();
    String getPayloadJson();
    Instant getOccurredAt();
    Instant getCreatedAt();
    Integer getRetryCount();
}
