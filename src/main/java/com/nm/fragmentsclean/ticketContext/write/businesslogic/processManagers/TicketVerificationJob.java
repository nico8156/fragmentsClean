package com.nm.fragmentsclean.ticketContext.write.businesslogic.processManagers;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Durable coordination state for one accepted ticket-verification event. */
public final class TicketVerificationJob {
    public enum State { PENDING, RUNNING, RETRY_PENDING, COMPLETED, FAILED_FINAL }

    private final UUID jobId;
    private final UUID commandId;
    private final UUID ticketId;
    private final UUID userId;
    private final String ocrText;
    private final String imageRef;
    private final Instant clientAt;
    private State state;
    private int attempts;
    private String leaseOwner;
    private Instant leaseUntil;
    private Instant nextAttemptAt;
    private String lastFailure;
    private long version;
    private final Instant createdAt;
    private Instant updatedAt;

    private TicketVerificationJob(Snapshot snapshot) {
        this.jobId = Objects.requireNonNull(snapshot.jobId());
        this.commandId = Objects.requireNonNull(snapshot.commandId());
        this.ticketId = Objects.requireNonNull(snapshot.ticketId());
        this.userId = Objects.requireNonNull(snapshot.userId());
        this.ocrText = snapshot.ocrText();
        this.imageRef = snapshot.imageRef();
        this.clientAt = snapshot.clientAt();
        this.state = Objects.requireNonNull(snapshot.state());
        if (snapshot.attempts() < 0 || snapshot.version() < 0) throw new IllegalArgumentException("Invalid job counters");
        this.attempts = snapshot.attempts();
        this.leaseOwner = snapshot.leaseOwner();
        this.leaseUntil = snapshot.leaseUntil();
        this.nextAttemptAt = Objects.requireNonNull(snapshot.nextAttemptAt());
        this.lastFailure = snapshot.lastFailure();
        this.version = snapshot.version();
        this.createdAt = Objects.requireNonNull(snapshot.createdAt());
        this.updatedAt = Objects.requireNonNull(snapshot.updatedAt());
    }

    public static TicketVerificationJob request(UUID jobId, UUID commandId, UUID ticketId, UUID userId,
            String ocrText, String imageRef, Instant clientAt, Instant now) {
        return new TicketVerificationJob(new Snapshot(jobId, commandId, ticketId, userId, ocrText, imageRef,
                clientAt, State.PENDING, 0, null, null, now, null, 0, now, now));
    }

    public static TicketVerificationJob reconstitute(Snapshot snapshot) { return new TicketVerificationJob(snapshot); }

    public boolean claimableAt(Instant now) {
        Objects.requireNonNull(now);
        return state == State.PENDING
                || (state == State.RETRY_PENDING && !now.isBefore(nextAttemptAt))
                || (state == State.RUNNING && leaseUntil != null && !now.isBefore(leaseUntil));
    }

    public void claim(String owner, Instant now, Duration leaseDuration) {
        requireText(owner, "owner");
        Objects.requireNonNull(now); Objects.requireNonNull(leaseDuration);
        if (leaseDuration.isZero() || leaseDuration.isNegative()) throw new IllegalArgumentException("leaseDuration must be positive");
        if (!claimableAt(now)) throw new IllegalStateException("Verification job is not claimable; an active lease or delay remains");
        state = State.RUNNING;
        attempts++;
        leaseOwner = owner;
        leaseUntil = now.plus(leaseDuration);
        touch(now);
    }

    public void retry(String owner, String failure, Instant now, Instant retryAt) {
        requireLease(owner);
        requireText(failure, "failure");
        if (retryAt.isBefore(now)) throw new IllegalArgumentException("retryAt must not be before now");
        state = State.RETRY_PENDING;
        lastFailure = failure;
        nextAttemptAt = retryAt;
        clearLease();
        touch(now);
    }

    public void complete(String owner, Instant now) { terminal(owner, State.COMPLETED, null, now); }
    public void failFinal(String owner, String failure, Instant now) { terminal(owner, State.FAILED_FINAL, requireText(failure, "failure"), now); }

    private void terminal(String owner, State target, String failure, Instant now) {
        requireLease(owner);
        state = target;
        lastFailure = failure;
        clearLease();
        touch(now);
    }

    private void requireLease(String owner) {
        if (state != State.RUNNING) throw new IllegalStateException("Verification job is not running");
        if (!Objects.equals(leaseOwner, owner)) throw new IllegalStateException("Verification lease owner mismatch");
    }
    private void clearLease() { leaseOwner = null; leaseUntil = null; }
    private void touch(Instant now) { updatedAt = Objects.requireNonNull(now); version++; }
    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }

    public Snapshot snapshot() { return new Snapshot(jobId, commandId, ticketId, userId, ocrText, imageRef, clientAt,
            state, attempts, leaseOwner, leaseUntil, nextAttemptAt, lastFailure, version, createdAt, updatedAt); }

    public record Snapshot(UUID jobId, UUID commandId, UUID ticketId, UUID userId, String ocrText, String imageRef,
            Instant clientAt, State state, int attempts, String leaseOwner, Instant leaseUntil, Instant nextAttemptAt,
            String lastFailure, long version, Instant createdAt, Instant updatedAt) { }
}
