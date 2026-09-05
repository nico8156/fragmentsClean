package com.nm.fragmentsclean.articleContext.write.businesslogic.models;

import java.util.Objects;

public final class ArticleTitle {

    private static final int MAX_LENGTH = 140;
    private final String value;

    private ArticleTitle(String value) {
        this.value = value;
    }

    public static ArticleTitle from(String raw) {
        var value = requireText(raw, "Le titre est obligatoire.");
        if (value.length() > MAX_LENGTH) {
            throw new ArticleDomainException("Le titre ne peut pas dépasser " + MAX_LENGTH + " caractères.");
        }
        return new ArticleTitle(value);
    }

    public String value() {
        return value;
    }

    private static String requireText(String raw, String message) {
        var value = Objects.requireNonNull(raw, message).trim();
        if (value.isEmpty()) {
            throw new ArticleDomainException(message);
        }
        return value;
    }
}
