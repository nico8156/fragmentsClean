package com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.repositorie.jpa.entities;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.Article;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Entity(name = "articles") // table "articles" dans ton schema.sql
@Getter
@NoArgsConstructor // obligatoire pour JPA
@ToString
@EqualsAndHashCode
public class ArticleJpaEntity {

    @Id
    private UUID articleId;

    private String slug;
    private String locale;

    private UUID authorId;
    private String authorName;

    private String title;
    private String intro;
    private String blocksJson;   // ArticleBlock[] sérialisé
    private String conclusion;

    private String coverUrl;
    private Integer coverWidth;
    private Integer coverHeight;
    private String coverAlt;

    private String tagsJson;     // string[] sérialisé
    private Integer readingTimeMin;

    private String coffeeIdsJson; // CoffeeId[] sérialisé

    private Instant createdAt;
    private Instant updatedAt;
    private Instant publishedAt;

    @Enumerated(EnumType.STRING)
    private ArticleStatus status;

    private long version;

    public ArticleJpaEntity(
            UUID articleId,
            String slug,
            String locale,
            UUID authorId,
            String authorName,
            String title,
            String intro,
            String blocksJson,
            String conclusion,
            String coverUrl,
            Integer coverWidth,
            Integer coverHeight,
            String coverAlt,
            String tagsJson,
            Integer readingTimeMin,
            String coffeeIdsJson,
            Instant createdAt,
            Instant updatedAt,
            Instant publishedAt,
            ArticleStatus status,
            long version
    ) {
        this.articleId = articleId;
        this.slug = slug;
        this.locale = locale;
        this.authorId = authorId;
        this.authorName = authorName;
        this.title = title;
        this.intro = intro;
        this.blocksJson = blocksJson;
        this.conclusion = conclusion;
        this.coverUrl = coverUrl;
        this.coverWidth = coverWidth;
        this.coverHeight = coverHeight;
        this.coverAlt = coverAlt;
        this.tagsJson = tagsJson;
        this.readingTimeMin = readingTimeMin;
        this.coffeeIdsJson = coffeeIdsJson;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.publishedAt = publishedAt;
        this.status = status;
        this.version = version;
    }

    // ─────────────────────────────────────────────
    // Mapping domaine <-> JPA via snapshot
    // ─────────────────────────────────────────────

    public static ArticleJpaEntity fromDomain(Article article,
                                              String tagsJson,
                                              String coffeeIdsJson) {
        var snap = article.toSnapshot();
        return new ArticleJpaEntity(
                snap.articleId(),
                snap.slug(),
                snap.locale(),
                snap.authorId(),
                snap.authorName(),
                snap.title(),
                snap.intro(),
                snap.blocksJson(),
                snap.conclusion(),
                snap.coverUrl(),
                snap.coverWidth(),
                snap.coverHeight(),
                snap.coverAlt(),
                tagsJson,
                snap.readingTimeMin(),
                coffeeIdsJson,
                snap.createdAt(),
                snap.updatedAt(),
                snap.publishedAt(),
                snap.status(),
                snap.version()
        );
    }

    public Article toDomain() {
        // Ici on suppose que tagsJson / coffeeIdsJson sont déjà au bon format JSON
        // et qu'un mapper les transformera côté repo si tu veux les List<String>/List<UUID>.
        // Si tu préfères, tu peux aussi les désérialiser ici.
        return Article.fromSnapshot(
                new Article.ArticleSnapshot(
                        this.articleId,
                        this.slug,
                        this.locale,
                        this.authorId,
                        this.authorName,
                        this.title,
                        this.intro,
                        this.blocksJson,
                        this.conclusion,
                        this.coverUrl,
                        this.coverWidth,
                        this.coverHeight,
                        this.coverAlt,
                        /* tags */ null,        // à remplacer par désérialisation de tagsJson
                        this.readingTimeMin,
                        /* coffeeIds */ null,   // à remplacer par désérialisation de coffeeIdsJson
                        this.createdAt,
                        this.updatedAt,
                        this.publishedAt,
                        this.status,
                        this.version
                )
        );
    }
}
