package com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Durable temporal intent. Article lifecycle invariants remain in articleContext. */
public final class EditorialPublicationSchedule {
    public enum Operation { PUBLISH, ARCHIVE }
    public enum Status { SCHEDULED, CLAIMED, DISPATCHED, COMPLETED, REJECTED, CANCELLED }

    private final UUID id;
    private final UUID articleId;
    private final UUID revisionId;
    private final Operation operation;
    private final Instant dueAt;
    private final Instant createdAt;
    private Status status;
    private String leaseOwner;
    private Instant leaseUntil;
    private String rejectionReason;
    private long version;

    private EditorialPublicationSchedule(UUID id, UUID articleId, UUID revisionId,
                                         Operation operation, Instant dueAt, Instant createdAt,
                                         Status status, String leaseOwner, Instant leaseUntil,
                                         String rejectionReason, long version) {
        this.id = Objects.requireNonNull(id);
        this.articleId = Objects.requireNonNull(articleId);
        this.revisionId = revisionId;
        this.operation = Objects.requireNonNull(operation);
        if (operation == Operation.PUBLISH && revisionId == null) {
            throw new IllegalArgumentException("Publication requires an approved revision");
        }
        this.dueAt = Objects.requireNonNull(dueAt);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.status = Objects.requireNonNull(status);
        this.leaseOwner = leaseOwner;
        this.leaseUntil = leaseUntil;
        this.rejectionReason = rejectionReason;
        if (version < 0) throw new IllegalArgumentException("Version cannot be negative");
        this.version = version;
    }

    public static EditorialPublicationSchedule schedule(UUID id, UUID articleId, UUID revisionId,
                                                        Operation operation, Instant dueAt, Instant now) {
        if (dueAt.isBefore(now)) throw new IllegalArgumentException("Schedule cannot be in the past");
        return new EditorialPublicationSchedule(id, articleId, revisionId, operation, dueAt, now,
                Status.SCHEDULED, null, null, null, 0);
    }

    public static EditorialPublicationSchedule reconstitute(Snapshot snapshot) {
        return new EditorialPublicationSchedule(snapshot.id(), snapshot.articleId(), snapshot.revisionId(),
                snapshot.operation(), snapshot.dueAt(), snapshot.createdAt(), snapshot.status(),
                snapshot.leaseOwner(), snapshot.leaseUntil(), snapshot.rejectionReason(), snapshot.version());
    }

    public boolean isDueAt(Instant now) {
        return status == Status.SCHEDULED && !dueAt.isAfter(now)
                || status == Status.CLAIMED && leaseUntil != null && !leaseUntil.isAfter(now);
    }

    public void claim(String worker, Instant now, Instant until) {
        requireWorker(worker);
        if (!until.isAfter(now)) throw new IllegalArgumentException("Lease must end after claim time");
        if (!isDueAt(now)) throw new IllegalStateException("Schedule is not claimable");
        status = Status.CLAIMED;
        leaseOwner = worker;
        leaseUntil = until;
        version++;
    }

    public void markDispatched(String worker, Instant now) {
        requireActiveLease(worker, now);
        status = Status.DISPATCHED;
        leaseOwner = null;
        leaseUntil = null;
        version++;
    }

    public void complete() {
        if (status == Status.COMPLETED) return;
        if (status != Status.DISPATCHED) throw new IllegalStateException("Only dispatched work can complete");
        status = Status.COMPLETED;
        version++;
    }

    public void reject(String reason) {
        if (status == Status.REJECTED) return;
        if (status != Status.DISPATCHED) throw new IllegalStateException("Only dispatched work can be rejected");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Rejection reason is required");
        status = Status.REJECTED;
        rejectionReason = reason.trim();
        version++;
    }

    public void cancel() {
        if (status != Status.SCHEDULED) throw new IllegalStateException("Only scheduled work can be cancelled");
        status = Status.CANCELLED;
        version++;
    }

    private void requireActiveLease(String worker, Instant now) {
        requireWorker(worker);
        if (status != Status.CLAIMED || !worker.equals(leaseOwner)
                || leaseUntil == null || !leaseUntil.isAfter(now)) {
            throw new IllegalStateException("Worker does not own an active lease");
        }
    }

    private static void requireWorker(String worker) {
        if (worker == null || worker.isBlank()) throw new IllegalArgumentException("Worker is required");
    }

    public Snapshot snapshot() {
        return new Snapshot(id, articleId, revisionId, operation, dueAt, status, leaseOwner,
                leaseUntil, rejectionReason, createdAt, version);
    }

    public record Snapshot(UUID id, UUID articleId, UUID revisionId, Operation operation,
                           Instant dueAt, Status status, String leaseOwner, Instant leaseUntil,
                           String rejectionReason, Instant createdAt, long version) { }
}
