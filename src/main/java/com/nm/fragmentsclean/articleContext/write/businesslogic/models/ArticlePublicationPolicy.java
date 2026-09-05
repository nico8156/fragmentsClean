package com.nm.fragmentsclean.articleContext.write.businesslogic.models;

/** The publication invariant shared by every publication entry point. */
public final class ArticlePublicationPolicy {
    public static final int DEFAULT_WARNING_THRESHOLD = 24;
    public static final int DEFAULT_HARD_LIMIT = 30;

    private final int warningThreshold;
    private final int hardLimit;

    public ArticlePublicationPolicy() {
        this(DEFAULT_WARNING_THRESHOLD, DEFAULT_HARD_LIMIT);
    }

    public ArticlePublicationPolicy(int warningThreshold, int hardLimit) {
        if (warningThreshold < 0 || hardLimit <= warningThreshold) {
            throw new IllegalArgumentException("Publication thresholds are invalid");
        }
        this.warningThreshold = warningThreshold;
        this.hardLimit = hardLimit;
    }

    public Decision evaluate(int publishedCount) {
        if (publishedCount < 0) {
            throw new IllegalArgumentException("Published count cannot be negative");
        }
        if (publishedCount >= hardLimit) {
            throw new ArticleDomainException("Le nombre maximal d'articles publiés est atteint.");
        }
        return new Decision(publishedCount, publishedCount >= warningThreshold, hardLimit);
    }

    public record Decision(int publishedCount, boolean warning, int hardLimit) {
    }
}
