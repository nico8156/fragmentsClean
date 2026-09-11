package com.nm.fragmentsclean.ticketContext.write.businesslogic.gateways;

import com.nm.fragmentsclean.ticketContext.write.businesslogic.models.TicketSubmissionFingerprint;
import java.time.Instant;
import java.util.UUID;

public interface TicketSubmissionFingerprintRegistry {
    boolean claim(TicketSubmissionFingerprint fingerprint, UUID ticketId, UUID userId, Instant claimedAt);
}
