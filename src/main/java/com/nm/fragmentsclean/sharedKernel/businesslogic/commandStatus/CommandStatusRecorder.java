package com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus;

import java.time.Instant;
import java.util.UUID;

public interface CommandStatusRecorder {
    void markApplied(UUID commandId, String aggregateType, String aggregateId, String eventType, Instant appliedAt);

    default boolean isApplied(UUID commandId) {
        return false;
    }
}
