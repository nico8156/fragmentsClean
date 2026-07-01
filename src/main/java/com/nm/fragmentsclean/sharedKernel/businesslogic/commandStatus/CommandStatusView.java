package com.nm.fragmentsclean.sharedKernel.businesslogic.commandStatus;

import java.time.Instant;

public record CommandStatusView(
        String status,
        Instant appliedAt,
        Instant rejectedAt,
        String reason
) {
}
