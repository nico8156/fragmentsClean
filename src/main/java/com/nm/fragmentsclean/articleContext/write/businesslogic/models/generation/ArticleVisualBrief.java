package com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation;

import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleDomainException;

import java.util.Objects;

public final class ArticleVisualBrief {
    private static final int MAX_LENGTH = 700;
    private final String value;
    private ArticleVisualBrief(String value) { this.value = value; }
    public static ArticleVisualBrief from(String raw) {
        var value = Objects.requireNonNull(raw, "Le brief visuel est obligatoire.").trim();
        if (value.isEmpty() || value.length() > MAX_LENGTH) throw new ArticleDomainException("Le brief visuel est invalide.");
        return new ArticleVisualBrief(value);
    }
    public String value() { return value; }
}
