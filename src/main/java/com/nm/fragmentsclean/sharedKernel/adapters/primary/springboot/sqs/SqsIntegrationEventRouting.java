package com.nm.fragmentsclean.sharedKernel.adapters.primary.springboot.sqs;

import com.nm.fragmentsclean.sharedKernel.businesslogic.eventing.IntegrationEventEnvelope;
import java.time.Instant;
import java.util.Objects;

public interface SqsIntegrationEventRouting {
    Result route(IntegrationEventEnvelope envelope);

    record Result(Status status, Instant retryAt) {
        public Result {
            Objects.requireNonNull(status, "status is required");
            if (status == Status.BUSY) Objects.requireNonNull(retryAt, "retryAt is required when busy");
            if (status != Status.BUSY && retryAt != null) {
                throw new IllegalArgumentException("retryAt is only allowed when busy");
            }
        }

        public static Result processed() { return new Result(Status.PROCESSED, null); }
        public static Result alreadyProcessed() { return new Result(Status.ALREADY_PROCESSED, null); }
        public static Result busyUntil(Instant retryAt) { return new Result(Status.BUSY, retryAt); }

        enum Status { PROCESSED, ALREADY_PROCESSED, BUSY }
    }
}
