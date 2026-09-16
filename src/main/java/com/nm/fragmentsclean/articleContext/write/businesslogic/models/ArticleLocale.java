package com.nm.fragmentsclean.articleContext.write.businesslogic.models;

/** Article language identity. Legacy short tags refer to the existing product locales. */
public record ArticleLocale(String value) {
    public ArticleLocale {
        if (value == null || value.isBlank()) throw new ArticleDomainException("La locale est obligatoire.");
        value = switch (value.trim()) {
            case "fr", "fr-FR" -> "fr-FR";
            case "en", "en-US" -> "en-US";
            default -> value.trim();
        };
    }

    public String legacyTag() {
        return switch (value) {
            case "fr-FR" -> "fr";
            case "en-US" -> "en";
            default -> value;
        };
    }
}
