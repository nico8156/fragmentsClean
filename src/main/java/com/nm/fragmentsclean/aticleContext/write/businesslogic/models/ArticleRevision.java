package com.nm.fragmentsclean.aticleContext.write.businesslogic.models;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class ArticleRevision {

    private final UUID revisionId;
    private ArticleContent content;
    private ArticleRevisionStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant publishedAt;

    private ArticleRevision(UUID revisionId, ArticleContent content, Instant now) {
        this.revisionId = Objects.requireNonNull(revisionId, "L'identifiant de révision est obligatoire.");
        this.content = Objects.requireNonNull(content, "Le contenu est obligatoire.");
        this.createdAt = Objects.requireNonNull(now, "La date de création est obligatoire.");
        this.updatedAt = now;
        this.status = ArticleRevisionStatus.DRAFT;
    }

    public static ArticleRevision draft(UUID revisionId, ArticleContent content, Instant now) {
        return new ArticleRevision(revisionId, content, now);
    }

    public static ArticleRevision reconstitute(UUID revisionId, ArticleContent content,
                                               ArticleRevisionStatus status, Instant createdAt,
                                               Instant updatedAt, Instant publishedAt) {
        var revision = new ArticleRevision(revisionId, content, createdAt);
        revision.status = Objects.requireNonNull(status, "Le statut de révision est obligatoire.");
        revision.updatedAt = Objects.requireNonNull(updatedAt, "La date de modification est obligatoire.");
        revision.publishedAt = publishedAt;
        return revision;
    }

    public void replaceContent(ArticleContent replacement, Instant now) {
        ensureEditable();
        this.content = Objects.requireNonNull(replacement, "Le contenu est obligatoire.");
        this.updatedAt = Objects.requireNonNull(now, "La date de modification est obligatoire.");
    }

    public void submitForReview(Instant now) {
        ensureStatus(ArticleRevisionStatus.DRAFT, "Seule une révision brouillon peut être soumise.");
        content.validateForReview();
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
        ensureStatus(ArticleRevisionStatus.PUBLISHED, "Seule une révision publiée peut être archivée.");
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
    public ArticleContent content() { return content; }
    public ArticleRevisionStatus status() { return status; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public Instant publishedAt() { return publishedAt; }
}
