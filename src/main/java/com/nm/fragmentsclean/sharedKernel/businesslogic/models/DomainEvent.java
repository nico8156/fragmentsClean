package com.nm.fragmentsclean.sharedKernel.businesslogic.models;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {
    UUID eventId();

    Instant occurredAt();
}
