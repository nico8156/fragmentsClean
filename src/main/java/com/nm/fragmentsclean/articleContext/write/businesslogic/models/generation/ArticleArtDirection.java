package com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation;

/** Article-owned visual intention. Provider-specific prompt language stays in adapters. */
public enum ArticleArtDirection {
    ORIGINAL, INTIMATE, LIVELY, CONTEMPLATIVE, BOLD;

    public static ArticleArtDirection from(String value) {
        if (value == null || value.isBlank()) return ORIGINAL;
        try { return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT)); }
        catch (IllegalArgumentException e) { throw new com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleDomainException("Unknown art direction: " + value); }
    }
}
