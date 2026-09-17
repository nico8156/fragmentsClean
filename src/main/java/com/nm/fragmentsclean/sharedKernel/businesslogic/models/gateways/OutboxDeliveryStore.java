package com.nm.fragmentsclean.sharedKernel.businesslogic.models.gateways;

import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.OutboxMessage;
import java.time.Instant;
import java.util.List;

/** Short, transactional persistence operations surrounding non-transactional delivery. */
public interface OutboxDeliveryStore {
    List<OutboxMessage> claimDue(String owner, Instant now, Instant leaseUntil, int limit);

    boolean markSent(OutboxMessage message, String owner, Instant completedAt);

    boolean recordFailure(long id, String owner, int failureCount, Instant nextAttemptAt,
                          String errorMessage, boolean terminal);
}
