package com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus;

import java.time.Instant;

public record CommandReceipt(
        CommandDescriptor descriptor,
        CommandReceiptStatus status,
        Instant appliedAt,
        Instant rejectedAt,
        String rejectionCode,
        String reason
) {
}
