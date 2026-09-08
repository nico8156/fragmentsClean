package com.nm.fragmentsclean.editorialIntelligenceContext.write.businesslogic.models;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Owns cadence, local failure policy and the durable lease for one editorial source. */
public final class EditorialSource {
    private final UUID id;
    private String name;
    private EditorialSourceAccessMode accessMode;
    private EditorialAuthorityLevel authorityLevel;
    private String endpoint;
    private Duration pollingFrequency;
    private boolean enabled;
    private EditorialSourceStatus status;
    private Instant lastCheckedAt;
    private Instant lastSuccessfulCheckAt;
    private Instant nextCheckAt;
    private int failureCount;
    private String leaseOwner;
    private Instant leaseUntil;
    private SourceCheckpoint checkpoint;
    private long version;

    private EditorialSource(UUID id, String name, EditorialSourceAccessMode accessMode,
                            EditorialAuthorityLevel authorityLevel, String endpoint, Duration pollingFrequency,
                            boolean enabled, EditorialSourceStatus status, Instant lastCheckedAt,
                            Instant lastSuccessfulCheckAt, Instant nextCheckAt, int failureCount,
                            String leaseOwner, Instant leaseUntil, SourceCheckpoint checkpoint, long version) {
        this.id = required(id, "id"); this.name = text(name, "name");
        this.accessMode = required(accessMode, "accessMode"); this.authorityLevel = required(authorityLevel, "authorityLevel");
        this.endpoint = text(endpoint, "endpoint"); this.pollingFrequency = validFrequency(pollingFrequency);
        this.enabled = enabled; this.status = required(status, "status"); this.lastCheckedAt = lastCheckedAt;
        this.lastSuccessfulCheckAt = lastSuccessfulCheckAt; this.nextCheckAt = required(nextCheckAt, "nextCheckAt");
        if (failureCount < 0 || version < 0) throw new IllegalArgumentException("Invalid source counters");
        this.failureCount = failureCount; this.leaseOwner = leaseOwner; this.leaseUntil = leaseUntil;
        this.checkpoint = checkpoint == null ? SourceCheckpoint.empty() : checkpoint; this.version = version;
    }

    public static EditorialSource register(UUID id, String name, EditorialSourceAccessMode accessMode,
                                           EditorialAuthorityLevel authorityLevel, String endpoint,
                                           Duration pollingFrequency, Instant now) {
        return new EditorialSource(id, name, accessMode, authorityLevel, endpoint, pollingFrequency,
                true, EditorialSourceStatus.HEALTHY, null, null, required(now, "now"), 0,
                null, null, SourceCheckpoint.empty(), 0);
    }

    public static EditorialSource reconstitute(Snapshot snapshot) {
        return new EditorialSource(snapshot.id(), snapshot.name(), snapshot.accessMode(), snapshot.authorityLevel(),
                snapshot.endpoint(), snapshot.pollingFrequency(), snapshot.enabled(), snapshot.status(),
                snapshot.lastCheckedAt(), snapshot.lastSuccessfulCheckAt(), snapshot.nextCheckAt(),
                snapshot.failureCount(), snapshot.leaseOwner(), snapshot.leaseUntil(), snapshot.checkpoint(), snapshot.version());
    }

    public boolean dueAt(Instant now) { return enabled && !now.isBefore(nextCheckAt) && !hasActiveLeaseAt(now); }

    public void claimConsultation(String worker, Instant now, Instant until) {
        if (!dueAt(now) && !leaseExpiredAt(now)) throw new IllegalStateException("Source is not due or its lease is still active");
        this.leaseOwner = text(worker, "worker"); this.leaseUntil = required(until, "until");
        if (!until.isAfter(now)) throw new IllegalArgumentException("Lease must end after claim time");
        this.lastCheckedAt = now; touch();
    }

    public void completeConsultation(int discoveredCount, String etag, String lastModified, String lastExternalId, Instant lastPublishedAt, Instant now) {
        completeConsultation(leaseOwner, discoveredCount, etag, lastModified, lastExternalId, lastPublishedAt, now);
    }

    public void completeConsultation(String worker, int discoveredCount, String etag, String lastModified, String lastExternalId, Instant lastPublishedAt, Instant now) {
        verifyLeaseOwner(worker, now);
        if (discoveredCount < 0) throw new IllegalArgumentException("discoveredCount must not be negative");
        checkpoint = new SourceCheckpoint(etag, lastModified, lastExternalId, lastPublishedAt);
        lastSuccessfulCheckAt = now; nextCheckAt = now.plus(pollingFrequency); failureCount = 0;
        status = EditorialSourceStatus.HEALTHY; clearLease(); touch();
    }

    public void failConsultation(String failureCategory, Instant now) {
        failConsultation(leaseOwner, failureCategory, now);
    }

    public void failConsultation(String worker, String failureCategory, Instant now) {
        verifyLeaseOwner(worker, now); text(failureCategory, "failureCategory");
        failureCount++; status = EditorialSourceStatus.DEGRADED;
        nextCheckAt = now.plus(backoffFor(failureCount)); clearLease(); touch();
    }

    /** Operator decision: changes the definition but never rewrites collected signals or checkpoints. */
    public void revise(String name, EditorialSourceAccessMode accessMode,
                       EditorialAuthorityLevel authorityLevel, String endpoint,
                       Duration pollingFrequency, Instant now) {
        if (hasActiveLeaseAt(required(now, "now"))) {
            throw new IllegalStateException("A source being consulted cannot be revised");
        }
        this.name = text(name, "name");
        this.accessMode = required(accessMode, "accessMode");
        this.authorityLevel = required(authorityLevel, "authorityLevel");
        this.endpoint = text(endpoint, "endpoint");
        this.pollingFrequency = validFrequency(pollingFrequency);
        this.nextCheckAt = now;
        touch();
    }

    public void enable(Instant now) {
        if (enabled) return;
        enabled = true; status = EditorialSourceStatus.HEALTHY; nextCheckAt = required(now, "now");
        touch();
    }

    public void disable() {
        if (!enabled) return;
        enabled = false; status = EditorialSourceStatus.DISABLED; clearLease(); touch();
    }

    public Snapshot snapshot() { return new Snapshot(id, name, accessMode, authorityLevel, endpoint, pollingFrequency, enabled, status, lastCheckedAt, lastSuccessfulCheckAt, nextCheckAt, failureCount, leaseOwner, leaseUntil, checkpoint, version); }

    private Duration backoffFor(int failures) { return Duration.ofHours(Math.min(24, 1L << Math.min(4, failures - 1))); }
    private boolean hasActiveLeaseAt(Instant now) { return leaseOwner != null && !leaseExpiredAt(now); }
    private boolean leaseExpiredAt(Instant now) { return leaseUntil == null || !now.isBefore(leaseUntil); }
    private void verifyLeaseOwner(String worker, Instant now) { if (worker == null || !worker.equals(leaseOwner) || leaseExpiredAt(now)) throw new IllegalStateException("Source consultation lease owner is no longer valid"); }
    private void clearLease() { leaseOwner = null; leaseUntil = null; }
    private void touch() { version++; }
    private static Duration validFrequency(Duration value) { if (value == null || value.isNegative() || value.isZero()) throw new IllegalArgumentException("pollingFrequency must be positive"); return value; }
    private static String text(String value, String field) { if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " must not be blank"); return value; }
    private static <T> T required(T value, String field) { return Objects.requireNonNull(value, field); }

    public record Snapshot(UUID id, String name, EditorialSourceAccessMode accessMode, EditorialAuthorityLevel authorityLevel,
                           String endpoint, Duration pollingFrequency, boolean enabled, EditorialSourceStatus status,
                           Instant lastCheckedAt, Instant lastSuccessfulCheckAt, Instant nextCheckAt, int failureCount,
                           String leaseOwner, Instant leaseUntil, SourceCheckpoint checkpoint, long version) { }
}
