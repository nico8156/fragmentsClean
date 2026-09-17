package com.nm.fragmentsclean.sharedKernel.businesslogic.eventing;

import java.time.Instant;
import java.util.Objects;

public record InboxClaim(Status status, String ownerToken, Instant leaseUntil) {

    public InboxClaim {
        Objects.requireNonNull(status, "status is required");
        if (status == Status.CLAIMED) {
            Objects.requireNonNull(ownerToken, "ownerToken is required for a claimed message");
            Objects.requireNonNull(leaseUntil, "leaseUntil is required for a claimed message");
        }
        if (status == Status.BUSY) {
            Objects.requireNonNull(leaseUntil, "leaseUntil is required for a busy message");
        }
        if (status != Status.CLAIMED && ownerToken != null) {
            throw new IllegalArgumentException("ownerToken is only allowed for a claimed message");
        }
        if (status == Status.ALREADY_PROCESSED && leaseUntil != null) {
            throw new IllegalArgumentException("leaseUntil is not allowed for an already processed message");
        }
    }

    public static InboxClaim acquired(String ownerToken, Instant leaseUntil) {
        return new InboxClaim(Status.CLAIMED, ownerToken, leaseUntil);
    }

    public static InboxClaim alreadyProcessed() {
        return new InboxClaim(Status.ALREADY_PROCESSED, null, null);
    }

    public static InboxClaim busyUntil(Instant leaseUntil) {
        return new InboxClaim(Status.BUSY, null, leaseUntil);
    }

    public boolean acquired() {
        return status == Status.CLAIMED;
    }

    public enum Status {
        CLAIMED,
        ALREADY_PROCESSED,
        BUSY
    }
}
