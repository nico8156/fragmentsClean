package com.nm.fragmentsclean.aticleContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.AggregateRoot;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Rich editorial aggregate introduced before the legacy persistence migration.
 * The existing Article class remains a compatibility adapter until phase 3.
 */
public final class ArticleAggregate extends AggregateRoot {

    private final String slug;
    private final String locale;
    private final UUID authorId;
    private final String authorName;
    private final Instant createdAt;
    private final List<ArticleRevision> revisions = new ArrayList<>();
    private UUID workingRevisionId;
    private UUID publishedRevisionId;
    private ArticleLifecycle lifecycle;
    private long version;

    private ArticleAggregate(UUID articleId,
                             String slug,
                             String locale,
                             UUID authorId,
                             String authorName,
                             ArticleRevision workingRevision,
                             Instant now) {
        super(Objects.requireNonNull(articleId, "L'identifiant article est obligatoire."));
        this.slug = requireText(slug, "Le slug est obligatoire.");
        this.locale = requireText(locale, "La locale est obligatoire.");
        this.authorId = Objects.requireNonNull(authorId, "L'auteur est obligatoire.");
        this.authorName = requireText(authorName, "Le nom de l'auteur est obligatoire.");
        this.createdAt = Objects.requireNonNull(now, "La date de création est obligatoire.");
        this.revisions.add(Objects.requireNonNull(workingRevision, "La révision est obligatoire."));
        this.workingRevisionId = workingRevision.revisionId();
        this.lifecycle = ArticleLifecycle.DRAFT;
    }

    private ArticleAggregate(UUID articleId, String slug, String locale, UUID authorId,
                             String authorName, Instant now) {
        super(Objects.requireNonNull(articleId, "L'identifiant article est obligatoire."));
        this.slug = requireText(slug, "Le slug est obligatoire.");
        this.locale = requireText(locale, "La locale est obligatoire.");
        this.authorId = Objects.requireNonNull(authorId, "L'auteur est obligatoire.");
        this.authorName = requireText(authorName, "Le nom de l'auteur est obligatoire.");
        this.createdAt = Objects.requireNonNull(now, "La date de création est obligatoire.");
        this.lifecycle = ArticleLifecycle.DRAFT;
    }

    private ArticleAggregate(UUID articleId, String slug, String locale, UUID authorId,
                             String authorName, Instant createdAt, List<ArticleRevision> revisions,
                             UUID workingRevisionId, UUID publishedRevisionId,
                             ArticleLifecycle lifecycle, long version) {
        super(Objects.requireNonNull(articleId, "L'identifiant article est obligatoire."));
        this.slug = requireText(slug, "Le slug est obligatoire.");
        this.locale = requireText(locale, "La locale est obligatoire.");
        this.authorId = Objects.requireNonNull(authorId, "L'auteur est obligatoire.");
        this.authorName = requireText(authorName, "Le nom de l'auteur est obligatoire.");
        this.createdAt = Objects.requireNonNull(createdAt, "La date de création est obligatoire.");
        this.revisions.addAll(List.copyOf(Objects.requireNonNull(revisions, "Les révisions sont obligatoires.")));
        this.workingRevisionId = workingRevisionId;
        this.publishedRevisionId = publishedRevisionId;
        this.lifecycle = Objects.requireNonNull(lifecycle, "Le cycle de vie est obligatoire.");
        if (version < 0) throw new ArticleDomainException("La version article est invalide.");
        this.version = version;
    }

    public static ArticleAggregate draft(UUID articleId,
                                         String slug,
                                         String locale,
                                         UUID authorId,
                                         String authorName,
                                         ArticleRevision workingRevision,
                                         Instant now) {
        return new ArticleAggregate(articleId, slug, locale, authorId, authorName, workingRevision, now);
    }

    public static ArticleAggregate awaitingGeneration(UUID articleId, String slug, String locale,
                                                       UUID authorId, String authorName, Instant now) {
        return new ArticleAggregate(articleId, slug, locale, authorId, authorName, now);
    }

    public static ArticleAggregate reconstitute(UUID articleId, String slug, String locale,
                                                UUID authorId, String authorName, Instant createdAt,
                                                List<ArticleRevision> revisions, UUID workingRevisionId,
                                                UUID publishedRevisionId, ArticleLifecycle lifecycle,
                                                long version) {
        return new ArticleAggregate(articleId, slug, locale, authorId, authorName, createdAt,
                revisions, workingRevisionId, publishedRevisionId, lifecycle, version);
    }

    public boolean awaitsGeneratedRevision() {
        return workingRevisionId == null && revisions.isEmpty() && lifecycle == ArticleLifecycle.DRAFT;
    }

    public void submitForReview(Instant now) {
        ensureLifecycle(ArticleLifecycle.DRAFT, "Seul un article brouillon peut être soumis.");
        workingRevision().submitForReview(now);
        lifecycle = ArticleLifecycle.IN_REVIEW;
    }

    public void registerDraftCreated(java.util.UUID commandId, Instant clientAt, Instant now) {
        registerEvent(new ArticleDraftCreatedEvent(
                UUID.randomUUID(), commandId, id, workingRevisionId, slug, locale, now, clientAt));
    }

    public void publishWorkingRevision(Instant now) {
        ensureLifecycle(ArticleLifecycle.IN_REVIEW, "Seul un article en revue peut être publié.");
        workingRevision().publish(now);
        publishedRevisionId = workingRevisionId;
        lifecycle = ArticleLifecycle.PUBLISHED;
        version++;
    }

    public void registerRevisionSubmitted(UUID commandId, Instant clientAt, Instant now) {
        registerEvent(new ArticleRevisionSubmittedEvent(
                UUID.randomUUID(), commandId, id, workingRevisionId, now, clientAt));
    }

    public void registerRevisionPublished(UUID commandId, Instant clientAt, Instant now) {
        registerEvent(new ArticleRevisionPublishedEvent(
                UUID.randomUUID(), commandId, id, publishedRevisionId, now, clientAt));
    }

    public UUID startWorkingRevision(UUID revisionId, ArticleContent content, Instant now) {
        if (lifecycle != ArticleLifecycle.PUBLISHED) {
            throw new ArticleDomainException("Une nouvelle révision démarre depuis un article publié.");
        }
        var revision = ArticleRevision.draft(revisionId, content, now);
        revisions.add(revision);
        workingRevisionId = revisionId;
        lifecycle = ArticleLifecycle.DRAFT;
        version++;
        return revisionId;
    }

    public ArticleRevision workingRevision() {
        return revisions.stream()
                .filter(revision -> revision.revisionId().equals(workingRevisionId))
                .findFirst()
                .orElseThrow(() -> new ArticleDomainException("La révision de travail est introuvable."));
    }

    public ArticleRevision publishedRevision() {
        if (publishedRevisionId == null) {
            throw new ArticleDomainException("Aucune révision publiée.");
        }
        return revisions.stream()
                .filter(revision -> revision.revisionId().equals(publishedRevisionId))
                .findFirst()
                .orElseThrow(() -> new ArticleDomainException("La révision publiée est introuvable."));
    }

    private void ensureLifecycle(ArticleLifecycle expected, String message) {
        if (lifecycle != expected) {
            throw new ArticleDomainException(message);
        }
    }

    private static String requireText(String value, String message) {
        var normalized = Objects.requireNonNull(value, message).trim();
        if (normalized.isEmpty()) {
            throw new ArticleDomainException(message);
        }
        return normalized;
    }

    public String slug() { return slug; }
    public String locale() { return locale; }
    public UUID authorId() { return authorId; }
    public String authorName() { return authorName; }
    public Instant createdAt() { return createdAt; }
    public List<ArticleRevision> revisions() { return List.copyOf(revisions); }
    public UUID workingRevisionId() { return workingRevisionId; }
    public UUID publishedRevisionId() { return publishedRevisionId; }
    public ArticleLifecycle lifecycle() { return lifecycle; }
    public long version() { return version; }
}
