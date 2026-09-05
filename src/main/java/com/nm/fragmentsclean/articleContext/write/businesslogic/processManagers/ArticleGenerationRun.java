package com.nm.fragmentsclean.articleContext.write.businesslogic.processManagers;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Durable attempt metadata. Provider content is materialised separately in the next phase. */
public final class ArticleGenerationRun {
    public enum Status { STARTED, SUCCEEDED, FAILED }
    private final UUID runId; private final UUID sagaId; private final int attempt;
    private final String workerId; private Status status; private String providerResponseId;
    private String provider; private String model; private String schemaVersion;
    private ArticleAuthoringFailureCategory failureCategory; private final Instant startedAt; private Instant completedAt;

    private ArticleGenerationRun(UUID runId, UUID sagaId, int attempt, String workerId, Status status,
                                 String providerResponseId, String provider, String model, String schemaVersion,
                                 ArticleAuthoringFailureCategory failureCategory, Instant startedAt, Instant completedAt) {
        this.runId=Objects.requireNonNull(runId); this.sagaId=Objects.requireNonNull(sagaId); this.attempt=attempt;
        this.workerId=require(workerId); this.status=Objects.requireNonNull(status); this.providerResponseId=providerResponseId;
        this.provider=provider; this.model=model; this.schemaVersion=schemaVersion; this.failureCategory=failureCategory;
        this.startedAt=Objects.requireNonNull(startedAt); this.completedAt=completedAt;
    }
    public static ArticleGenerationRun start(UUID runId, UUID sagaId, int attempt, String workerId, Instant now) {
        if (attempt < 1) throw new IllegalArgumentException("attempt must be positive");
        return new ArticleGenerationRun(runId, sagaId, attempt, workerId, Status.STARTED, null, null, null, null, null, now, null);
    }
    public static ArticleGenerationRun reconstitute(Snapshot s) { return new ArticleGenerationRun(s.runId(),s.sagaId(),s.attempt(),s.workerId(),s.status(),s.providerResponseId(),s.provider(),s.model(),s.schemaVersion(),s.failureCategory(),s.startedAt(),s.completedAt()); }
    public void succeed(String provider, String responseId, String model, String schemaVersion, Instant now) {
        if (status != Status.STARTED) throw new IllegalStateException("Generation run is already completed");
        this.provider=require(provider); this.providerResponseId=responseId; this.model=model; this.schemaVersion=require(schemaVersion); this.status=Status.SUCCEEDED; this.completedAt=Objects.requireNonNull(now);
    }
    public void fail(ArticleAuthoringFailureCategory category, Instant now) { if (status != Status.STARTED) throw new IllegalStateException("Generation run is already completed"); this.failureCategory=Objects.requireNonNull(category); this.status=Status.FAILED; this.completedAt=Objects.requireNonNull(now); }
    private static String require(String v) { if (v == null || v.isBlank()) throw new IllegalArgumentException("value must not be blank"); return v; }
    public Snapshot snapshot() { return new Snapshot(runId,sagaId,attempt,workerId,status,providerResponseId,provider,model,schemaVersion,failureCategory,startedAt,completedAt); }
    public record Snapshot(UUID runId, UUID sagaId, int attempt, String workerId, Status status, String providerResponseId, String provider, String model, String schemaVersion, ArticleAuthoringFailureCategory failureCategory, Instant startedAt, Instant completedAt) { }
}
