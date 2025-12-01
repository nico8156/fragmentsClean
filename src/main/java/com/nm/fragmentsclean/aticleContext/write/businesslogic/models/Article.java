package com.nm.fragmentsclean.aticleContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.AggregateRoot;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class Article extends AggregateRoot {

    private final String slug;
    private final String locale;

    private final UUID authorId;
    private final String authorName;

    private final Instant createdAt;
    private Instant updatedAt;
    private Instant publishedAt;

    private String title;
    private String intro;
    private String blocksJson;
    private String conclusion;

    private String coverUrl;
    private Integer coverWidth;
    private Integer coverHeight;
    private String coverAlt;

    private List<String> tags;
    private Integer readingTimeMin;
    private List<UUID> coffeeIds;

    private ArticleStatus status;
    private long version;

    private Article(UUID articleId,
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
                    List<String> tags,
                    Integer readingTimeMin,
                    List<UUID> coffeeIds,
                    Instant createdAt,
                    Instant updatedAt,
                    Instant publishedAt,
                    ArticleStatus status,
                    long version) {
        super(articleId);
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
        this.tags = tags;
        this.readingTimeMin = readingTimeMin;
        this.coffeeIds = coffeeIds;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.publishedAt = publishedAt;
        this.status = status;
        this.version = version;
    }

    public static Article createNew(UUID articleId,
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
                                    List<String> tags,
                                    Integer readingTimeMin,
                                    List<UUID> coffeeIds,
                                    Instant now) {
        return new Article(
                articleId,
                slug,
                locale,
                authorId,
                authorName,
                title,
                intro,
                blocksJson,
                conclusion,
                coverUrl,
                coverWidth,
                coverHeight,
                coverAlt,
                tags,
                readingTimeMin,
                coffeeIds,
                now,
                now,
                now,                       // publishedAt = now
                ArticleStatus.PUBLISHED,   // visible côté app directement
                0L
        );
    }

    public static Article fromSnapshot(ArticleSnapshot snap) {
        return new Article(
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
                snap.tags(),
                snap.readingTimeMin(),
                snap.coffeeIds(),
                snap.createdAt(),
                snap.updatedAt(),
                snap.publishedAt(),
                snap.status(),
                snap.version()
        );
    }

    public void registerCreatedEvent(UUID commandId,
                                     Instant clientAt,
                                     Instant serverNow) {
        registerEvent(new ArticleCreatedEvent(
                UUID.randomUUID(),   // eventId
                commandId,
                this.id,             // articleId
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
                this.tags,
                this.readingTimeMin,
                this.coffeeIds,
                this.status,
                this.version,
                serverNow,
                clientAt
        ));
    }

    public ArticleSnapshot toSnapshot() {
        return new ArticleSnapshot(
                this.id,
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
                this.tags,
                this.readingTimeMin,
                this.coffeeIds,
                this.createdAt,
                this.updatedAt,
                this.publishedAt,
                this.status,
                this.version
        );
    }

    public record ArticleSnapshot(
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
            List<String> tags,
            Integer readingTimeMin,
            List<UUID> coffeeIds,
            Instant createdAt,
            Instant updatedAt,
            Instant publishedAt,
            ArticleStatus status,
            long version
    ) {}
}
