package com.nm.fragmentsclean.articleContext.write.businesslogic.models;

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
    private Integer featuredRank;
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
        this.locale = new ArticleLocale(locale).value();
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
        this.locale = new ArticleLocale(locale).value();
        this.authorId = Objects.requireNonNull(authorId, "L'auteur est obligatoire.");
        this.authorName = requireText(authorName, "Le nom de l'auteur est obligatoire.");
        this.createdAt = Objects.requireNonNull(now, "La date de création est obligatoire.");
        this.lifecycle = ArticleLifecycle.DRAFT;
    }

    private ArticleAggregate(UUID articleId, String slug, String locale, UUID authorId,
                             String authorName, Instant createdAt, List<ArticleRevision> revisions,
                             UUID workingRevisionId, UUID publishedRevisionId,
                             ArticleLifecycle lifecycle, long version) {
        this(articleId, slug, locale, authorId, authorName, createdAt, revisions,
                workingRevisionId, publishedRevisionId, lifecycle, version, null);
    }

    private ArticleAggregate(UUID articleId, String slug, String locale, UUID authorId,
                             String authorName, Instant createdAt, List<ArticleRevision> revisions,
                             UUID workingRevisionId, UUID publishedRevisionId,
                             ArticleLifecycle lifecycle, long version, Integer featuredRank) {
        super(Objects.requireNonNull(articleId, "L'identifiant article est obligatoire."));
        this.slug = requireText(slug, "Le slug est obligatoire.");
        this.locale = new ArticleLocale(locale).value();
        this.authorId = Objects.requireNonNull(authorId, "L'auteur est obligatoire.");
        this.authorName = requireText(authorName, "Le nom de l'auteur est obligatoire.");
        this.createdAt = Objects.requireNonNull(createdAt, "La date de création est obligatoire.");
        this.revisions.addAll(List.copyOf(Objects.requireNonNull(revisions, "Les révisions sont obligatoires.")));
        this.workingRevisionId = workingRevisionId;
        this.publishedRevisionId = publishedRevisionId;
        this.lifecycle = Objects.requireNonNull(lifecycle, "Le cycle de vie est obligatoire.");
        if (featuredRank != null && (lifecycle != ArticleLifecycle.PUBLISHED || featuredRank < 1 || featuredRank > 5)) {
            throw new ArticleDomainException("La sélection à la une est invalide.");
        }
        this.featuredRank = featuredRank;
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

    public static ArticleAggregate reconstitute(UUID articleId, String slug, String locale,
                                                UUID authorId, String authorName, Instant createdAt,
                                                List<ArticleRevision> revisions, UUID workingRevisionId,
                                                UUID publishedRevisionId, ArticleLifecycle lifecycle,
                                                long version, Integer featuredRank) {
        return new ArticleAggregate(articleId, slug, locale, authorId, authorName, createdAt,
                revisions, workingRevisionId, publishedRevisionId, lifecycle, version, featuredRank);
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

    public void replaceWorkingDraft(ArticleRevisionDraft replacement, Instant now) {
        ensureLifecycle(ArticleLifecycle.DRAFT, "Seul un article brouillon peut être modifié.");
        workingRevision().replaceDraft(replacement, now);
        version++;
    }

    public void registerDraftEdited(UUID commandId, Instant clientAt, Instant now) {
        registerEvent(new ArticleDraftEditedEvent(
                UUID.randomUUID(), commandId, id, workingRevisionId, now, clientAt));
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
                UUID.randomUUID(), commandId, id, publishedRevisionId, version, now, clientAt));
    }

    public void archive(Instant now) {
        if (lifecycle == ArticleLifecycle.ARCHIVED) return;
        workingRevision().archive(now);
        lifecycle = ArticleLifecycle.ARCHIVED;
        featuredRank = null;
        version++;
    }

    public void registerArchived(UUID commandId, Instant clientAt, Instant now) {
        registerEvent(new ArticleArchivedEvent(
                UUID.randomUUID(), commandId, id, workingRevisionId, version, now, clientAt));
    }

    public void withdrawToDraft(UUID newRevisionId, Instant now) {
        ensureLifecycle(ArticleLifecycle.PUBLISHED, "Seul un article publié peut être dépublié.");
        if (revisions.stream().anyMatch(revision -> revision.revisionId().equals(newRevisionId))) {
            throw new ArticleDomainException("La nouvelle révision existe déjà.");
        }
        startWorkingRevision(newRevisionId, publishedRevision().draft(), now);
        featuredRank = null;
    }

    public void registerWithdrawn(UUID commandId, Instant clientAt, Instant now) {
        registerEvent(new ArticleWithdrawnEvent(UUID.randomUUID(), commandId, id,
                publishedRevisionId, workingRevisionId, version, now, clientAt));
    }

    public UUID startWorkingRevision(UUID revisionId, ArticleRevisionDraft draft, Instant now) {
        if (lifecycle != ArticleLifecycle.PUBLISHED) {
            throw new ArticleDomainException("Une nouvelle révision démarre depuis un article publié.");
        }
        var revision = ArticleRevision.draft(revisionId, draft, now);
        revisions.add(revision);
        workingRevisionId = revisionId;
        lifecycle = ArticleLifecycle.DRAFT;
        featuredRank = null;
        version++;
        return revisionId;
    }

    public boolean setFeaturedRank(Integer rank, Instant now) {
        ensureLifecycle(ArticleLifecycle.PUBLISHED, "Seul un article publié peut être à la une.");
        if (rank != null && (rank < 1 || rank > 5)) {
            throw new ArticleDomainException("Le rang à la une doit être compris entre 1 et 5.");
        }
        Objects.requireNonNull(now, "La date est obligatoire.");
        if (Objects.equals(featuredRank, rank)) return false;
        featuredRank = rank;
        version++;
        return true;
    }

    public void registerFeaturedRankChanged(UUID commandId, Instant clientAt, Instant now) {
        registerEvent(new ArticleFeaturedRankChangedEvent(UUID.randomUUID(), commandId, id,
                featuredRank, version, now, clientAt));
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
    public Integer featuredRank() { return featuredRank; }
}
