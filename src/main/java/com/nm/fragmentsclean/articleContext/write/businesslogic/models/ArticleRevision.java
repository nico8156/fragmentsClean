package com.nm.fragmentsclean.articleContext.write.businesslogic.models;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class ArticleRevision {

    private final UUID revisionId;
    private ArticleRevisionDraft draft;
    private ArticleRevisionStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant publishedAt;

    private ArticleRevision(UUID revisionId, ArticleRevisionDraft draft, Instant now) {
        this.revisionId = Objects.requireNonNull(revisionId, "L'identifiant de révision est obligatoire.");
        this.draft = Objects.requireNonNull(draft, "Le brouillon est obligatoire.");
        this.createdAt = Objects.requireNonNull(now, "La date de création est obligatoire.");
        this.updatedAt = now;
        this.status = ArticleRevisionStatus.DRAFT;
    }

    public static ArticleRevision draft(UUID revisionId, ArticleRevisionDraft draft, Instant now) {
        return new ArticleRevision(revisionId, draft, now);
    }

    public static ArticleRevision reconstitute(UUID revisionId, ArticleRevisionDraft draft,
                                               ArticleRevisionStatus status, Instant createdAt,
                                               Instant updatedAt, Instant publishedAt) {
        var revision = new ArticleRevision(revisionId, draft, createdAt);
        revision.status = Objects.requireNonNull(status, "Le statut de révision est obligatoire.");
        revision.updatedAt = Objects.requireNonNull(updatedAt, "La date de modification est obligatoire.");
        revision.publishedAt = publishedAt;
        return revision;
    }

    public void replaceDraft(ArticleRevisionDraft replacement, Instant now) {
        ensureEditable();
        this.draft = Objects.requireNonNull(replacement, "Le brouillon est obligatoire.");
        this.updatedAt = Objects.requireNonNull(now, "La date de modification est obligatoire.");
    }

    public void submitForReview(Instant now) {
        ensureStatus(ArticleRevisionStatus.DRAFT, "Seule une révision brouillon peut être soumise.");
        draft.validateForReview();
        this.status = ArticleRevisionStatus.IN_REVIEW;
        this.updatedAt = Objects.requireNonNull(now, "La date de modification est obligatoire.");
    }

    public void publish(Instant now) {
        ensureStatus(ArticleRevisionStatus.IN_REVIEW, "Seule une révision en revue peut être publiée.");
        this.status = ArticleRevisionStatus.PUBLISHED;
        this.publishedAt = Objects.requireNonNull(now, "La date de publication est obligatoire.");
        this.updatedAt = now;
    }

    public void archive(Instant now) {
        if (status == ArticleRevisionStatus.ARCHIVED) return;
        this.status = ArticleRevisionStatus.ARCHIVED;
        this.updatedAt = Objects.requireNonNull(now, "La date d'archivage est obligatoire.");
    }

    private void ensureEditable() {
        if (status != ArticleRevisionStatus.DRAFT) {
            throw new ArticleDomainException("Une révision non brouillon est immuable.");
        }
    }

    private void ensureStatus(ArticleRevisionStatus expected, String message) {
        if (status != expected) {
            throw new ArticleDomainException(message);
        }
    }

    public UUID revisionId() { return revisionId; }
    public ArticleRevisionDraft draft() { return draft; }
    public ArticleContent content() { return draft.content(); }
    public ArticleRevisionStatus status() { return status; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public Instant publishedAt() { return publishedAt; }
}
