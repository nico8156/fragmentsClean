package com.nm.fragmentsclean.aticleContext.write.businesslogic.processManagers;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleDomainException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Durable orchestration state. Business transitions are explicit; persistence never changes state generically. */
public final class ArticleAuthoringSaga {
    private final UUID sagaId;
    private final UUID articleId;
    private final UUID revisionId;
    private final String theme;
    private final ArticleAuthoringTrigger trigger;
    private ArticleAuthoringSagaState state;
    private long version;
    private int generationAttempts;
    private String leaseOwner;
    private Instant leaseUntil;
    private ArticleAuthoringFailureCategory failureCategory;
    private final Instant createdAt;
    private Instant updatedAt;

    private ArticleAuthoringSaga(UUID sagaId, UUID articleId, UUID revisionId, String theme,
                                 ArticleAuthoringTrigger trigger, ArticleAuthoringSagaState state,
                                 long version, int generationAttempts, String leaseOwner, Instant leaseUntil,
                                 ArticleAuthoringFailureCategory failureCategory, Instant createdAt, Instant updatedAt) {
        this.sagaId = require(sagaId, "sagaId"); this.articleId = require(articleId, "articleId");
        this.revisionId = require(revisionId, "revisionId"); this.theme = requireText(theme, "theme");
        this.trigger = Objects.requireNonNull(trigger); this.state = Objects.requireNonNull(state);
        if (version < 0 || generationAttempts < 0) throw new ArticleDomainException("Invalid saga counters");
        this.version = version; this.generationAttempts = generationAttempts; this.leaseOwner = leaseOwner;
        this.leaseUntil = leaseUntil; this.failureCategory = failureCategory;
        this.createdAt = require(createdAt, "createdAt"); this.updatedAt = require(updatedAt, "updatedAt");
    }

    public static ArticleAuthoringSaga request(UUID sagaId, UUID articleId, UUID revisionId, String theme,
                                               ArticleAuthoringTrigger trigger, Instant now) {
        return new ArticleAuthoringSaga(sagaId, articleId, revisionId, theme, trigger,
                ArticleAuthoringSagaState.REQUESTED, 0, 0, null, null, null, now, now);
    }

    public static ArticleAuthoringSaga reconstitute(Snapshot s) {
        return new ArticleAuthoringSaga(s.sagaId(), s.articleId(), s.revisionId(), s.theme(), s.trigger(), s.state(),
                s.version(), s.generationAttempts(), s.leaseOwner(), s.leaseUntil(), s.failureCategory(), s.createdAt(), s.updatedAt());
    }

    public void enqueueGeneration(Instant now) { transition(ArticleAuthoringSagaState.REQUESTED, ArticleAuthoringSagaState.GENERATION_PENDING, now); }

    public void claimGeneration(String owner, Instant now, Instant leaseUntil) {
        requireText(owner, "owner"); require(leaseUntil, "leaseUntil");
        if (state == ArticleAuthoringSagaState.GENERATING && !leaseExpiredAt(now)) reject("Generation lease is still active");
        else if (state != ArticleAuthoringSagaState.GENERATION_PENDING && state != ArticleAuthoringSagaState.GENERATING)
            reject("Cannot claim generation from " + state);
        this.state = ArticleAuthoringSagaState.GENERATING; this.generationAttempts++; this.leaseOwner = owner;
        this.leaseUntil = leaseUntil; touch(now);
    }

    public void recoverExpiredGeneration(Instant now) {
        if (state == ArticleAuthoringSagaState.GENERATING && leaseExpiredAt(now)) {
            state = ArticleAuthoringSagaState.GENERATION_PENDING; leaseOwner = null; leaseUntil = null; touch(now);
        }
    }
    public void startValidation(Instant now) { transition(ArticleAuthoringSagaState.GENERATING, ArticleAuthoringSagaState.VALIDATING, now); clearLease(); }
    public void retryGeneration(ArticleAuthoringFailureCategory category, Instant now) {
        Objects.requireNonNull(category, "category");
        if (state != ArticleAuthoringSagaState.GENERATING) reject("Cannot retry generation from " + state);
        state = ArticleAuthoringSagaState.GENERATION_PENDING;
        failureCategory = category;
        clearLease();
        touch(now);
    }
    public void markReadyForReview(Instant now) { transition(ArticleAuthoringSagaState.VALIDATING, ArticleAuthoringSagaState.READY_FOR_REVIEW, now); }
    public void requestNotification(Instant now) { transition(ArticleAuthoringSagaState.READY_FOR_REVIEW, ArticleAuthoringSagaState.NOTIFICATION_PENDING, now); }
    public void markNotified(Instant now) { transition(ArticleAuthoringSagaState.NOTIFICATION_PENDING, ArticleAuthoringSagaState.NOTIFIED, now); }
    public void beginEditing(Instant now) { if (state != ArticleAuthoringSagaState.NOTIFIED && state != ArticleAuthoringSagaState.READY_FOR_REVIEW) reject("Cannot edit from " + state); state = ArticleAuthoringSagaState.EDITING; touch(now); }
    public void requestPublication(Instant now) { if (state != ArticleAuthoringSagaState.EDITING && state != ArticleAuthoringSagaState.NOTIFIED && state != ArticleAuthoringSagaState.READY_FOR_REVIEW) reject("Cannot publish from " + state); state = ArticleAuthoringSagaState.PUBLICATION_REQUESTED; touch(now); }
    public void markPublished(Instant now) { transition(ArticleAuthoringSagaState.PUBLICATION_REQUESTED, ArticleAuthoringSagaState.PUBLISHED, now); }
    public void markFailed(ArticleAuthoringFailureCategory category, Instant now) { Objects.requireNonNull(category); if (terminal()) reject("Saga is terminal"); state = ArticleAuthoringSagaState.FAILED; failureCategory = category; clearLease(); touch(now); }
    public void reject(Instant now) { if (terminal()) reject("Saga is terminal"); state = ArticleAuthoringSagaState.REJECTED; clearLease(); touch(now); }
    public void expire(Instant now) { if (terminal()) reject("Saga is terminal"); state = ArticleAuthoringSagaState.EXPIRED; clearLease(); touch(now); }
    public void cancel(Instant now) { if (terminal()) reject("Saga is terminal"); state = ArticleAuthoringSagaState.CANCELLED; clearLease(); touch(now); }

    private boolean terminal() { return state == ArticleAuthoringSagaState.PUBLISHED || state == ArticleAuthoringSagaState.REJECTED || state == ArticleAuthoringSagaState.EXPIRED || state == ArticleAuthoringSagaState.CANCELLED; }
    private boolean leaseExpiredAt(Instant now) { return leaseUntil != null && !now.isBefore(leaseUntil); }
    private void transition(ArticleAuthoringSagaState from, ArticleAuthoringSagaState to, Instant now) { if (state != from) reject("Cannot transition from " + state + " to " + to); state = to; touch(now); }
    private void clearLease() { leaseOwner = null; leaseUntil = null; }
    private void touch(Instant now) { updatedAt = require(now, "now"); version++; }
    private static <T> T require(T value, String name) { return Objects.requireNonNull(value, name); }
    private static String requireText(String value, String name) { if (value == null || value.isBlank()) throw new ArticleDomainException(name + " must not be blank"); return value; }
    private static void reject(String message) { throw new ArticleDomainException(message); }

    public Snapshot snapshot() { return new Snapshot(sagaId, articleId, revisionId, theme, trigger, state, version, generationAttempts, leaseOwner, leaseUntil, failureCategory, createdAt, updatedAt); }
    public boolean leaseExpired(Instant now) { return leaseExpiredAt(now); }
    public record Snapshot(UUID sagaId, UUID articleId, UUID revisionId, String theme, ArticleAuthoringTrigger trigger, ArticleAuthoringSagaState state, long version, int generationAttempts, String leaseOwner, Instant leaseUntil, ArticleAuthoringFailureCategory failureCategory, Instant createdAt, Instant updatedAt) {}
}
