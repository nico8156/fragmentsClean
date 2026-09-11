package com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus;

import java.time.Instant;

/** Secondary port for durable, owner-scoped command receipts. */
public interface CommandReceiptStore {
    CommandReceipt registerPending(CommandDescriptor descriptor, Instant now);

    CommandReceipt lock(CommandDescriptor descriptor);

    void markApplied(CommandDescriptor descriptor, Instant appliedAt);

    void markRejected(CommandDescriptor descriptor, String rejectionCode, String reason, Instant rejectedAt);
}
