package com.nm.fragmentsclean.articleContext.write.businesslogic.models;

import java.util.List;
import java.util.Objects;

public final class ArticleContent {

    private final ArticleTitle title;
    private final ArticleIntroduction introduction;
    private final List<ArticleSection> sections;
    private final ArticleParagraph conclusion;

    private ArticleContent(ArticleTitle title,
                           ArticleIntroduction introduction,
                           List<ArticleSection> sections,
                           ArticleParagraph conclusion) {
        this.title = Objects.requireNonNull(title, "Le titre est obligatoire.");
        this.introduction = Objects.requireNonNull(introduction, "L'introduction est obligatoire.");
        this.sections = List.copyOf(sections);
        this.conclusion = Objects.requireNonNull(conclusion, "La conclusion est obligatoire.");
    }

    public static ArticleContent draft(ArticleTitle title,
                                       ArticleIntroduction introduction,
                                       List<ArticleSection> sections,
                                       ArticleParagraph conclusion) {
        var safeSections = List.copyOf(Objects.requireNonNull(sections, "Les sections sont obligatoires."));
        if (safeSections.isEmpty()) {
            throw new ArticleDomainException("Un article doit contenir au moins une section.");
        }
        return new ArticleContent(title, introduction, safeSections, conclusion);
    }

    public ArticleTitle title() {
        return title;
    }

    public ArticleIntroduction introduction() {
        return introduction;
    }

    public List<ArticleSection> sections() {
        return sections;
    }

    public ArticleParagraph conclusion() {
        return conclusion;
    }

    public void validateForReview() {
        if (sections.stream().anyMatch(section -> !section.hasContent())) {
            throw new ArticleDomainException("Chaque section doit contenir au moins un paragraphe.");
        }
    }
}
