package com.nm.fragmentsclean.sharedKernel.businesslogic.models;

import java.time.Instant;
import java.util.UUID;

public record CommandMetadata(
        UUID commandId,
        String clientId,
        Instant occurredAt
) {
}
