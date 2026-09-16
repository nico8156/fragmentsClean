package com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus;

import java.time.Instant;
import java.util.UUID;

/** Legacy Studio command ids are server-generated and read only by administrators. */
public interface AdminCommandRejectionRecorder {
    void reject(UUID commandId, String code, String reason, Instant at);
    java.util.Optional<String> rejectionReason(UUID commandId);
}
