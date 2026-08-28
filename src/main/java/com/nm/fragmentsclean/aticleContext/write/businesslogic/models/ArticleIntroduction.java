package com.nm.fragmentsclean.aticleContext.write.businesslogic.models;

import java.util.Objects;

public final class ArticleIntroduction {

    private static final int MAX_LENGTH = 2_000;
    private final String value;

    private ArticleIntroduction(String value) {
        this.value = value;
    }

    public static ArticleIntroduction from(String raw) {
        var value = Objects.requireNonNull(raw, "L'introduction est obligatoire.").trim();
        if (value.isEmpty()) {
            throw new ArticleDomainException("L'introduction est obligatoire.");
        }
        if (value.length() > MAX_LENGTH) {
            throw new ArticleDomainException("L'introduction ne peut pas dépasser " + MAX_LENGTH + " caractères.");
        }
        return new ArticleIntroduction(value);
    }

    public String value() {
        return value;
    }
}
