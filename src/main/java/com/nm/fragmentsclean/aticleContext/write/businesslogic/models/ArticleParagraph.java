package com.nm.fragmentsclean.aticleContext.write.businesslogic.models;

import java.util.Objects;

public final class ArticleParagraph {

    private static final int MAX_LENGTH = 5_000;
    private final String value;

    private ArticleParagraph(String value) {
        this.value = value;
    }

    public static ArticleParagraph from(String raw) {
        var value = Objects.requireNonNull(raw, "Le paragraphe est obligatoire.").trim();
        if (value.isEmpty()) {
            throw new ArticleDomainException("Le paragraphe est obligatoire.");
        }
        if (value.length() > MAX_LENGTH) {
            throw new ArticleDomainException("Un paragraphe ne peut pas dépasser " + MAX_LENGTH + " caractères.");
        }
        return new ArticleParagraph(value);
    }

    public String value() {
        return value;
    }
}
