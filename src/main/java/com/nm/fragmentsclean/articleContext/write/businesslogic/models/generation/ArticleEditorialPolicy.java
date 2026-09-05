package com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation;

import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleDomainException;

import java.util.HashSet;

public final class ArticleEditorialPolicy {
    public static final int MIN_SECTIONS = 3;
    public static final int MAX_SECTIONS = 4;
    private ArticleEditorialPolicy() {}

    public static void validate(GeneratedArticleDraft draft) {
        int count = draft.sections().size();
        if (count < MIN_SECTIONS || count > MAX_SECTIONS) throw new ArticleDomainException("Un article généré contient 3 ou 4 sections.");
        if (draft.sections().stream().anyMatch(s -> s.content().paragraphs().size() != 1))
            throw new ArticleDomainException("Chaque section générée contient exactement un paragraphe.");
        if (draft.tags().isEmpty() || draft.tags().size() > 3 || new HashSet<>(draft.tags()).size() != draft.tags().size())
            throw new ArticleDomainException("Un article généré contient entre 1 et 3 tags distincts.");
    }
}
