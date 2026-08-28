package com.nm.fragmentsclean.aticleContext.write.businesslogic.models.generation;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleDomainException;

import java.util.Objects;

public final class ArticleSubject {
    private static final int MAX_LENGTH = 240;
    private final String value;

    private ArticleSubject(String value) { this.value = value; }

    public static ArticleSubject from(String raw) {
        var value = Objects.requireNonNull(raw, "Le sujet est obligatoire.").trim();
        if (value.isEmpty() || value.length() > MAX_LENGTH || value.chars().anyMatch(Character::isISOControl)) {
            throw new ArticleDomainException("Le sujet de l'article est invalide.");
        }
        return new ArticleSubject(value);
    }

    public String value() { return value; }
}
