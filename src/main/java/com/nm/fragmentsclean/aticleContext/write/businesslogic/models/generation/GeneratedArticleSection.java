package com.nm.fragmentsclean.aticleContext.write.businesslogic.models.generation;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleParagraph;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleSection;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.ArticleImageRef;

import java.util.Objects;

public final class GeneratedArticleSection {
    private final ArticleSection content;
    private final ArticleVisualBrief visualBrief;

    private GeneratedArticleSection(ArticleSection content, ArticleVisualBrief visualBrief) {
        this.content = content; this.visualBrief = visualBrief;
    }
    public static GeneratedArticleSection from(String heading, String paragraph, String visualBrief) {
        return new GeneratedArticleSection(
                ArticleSection.draft(heading).withParagraph(ArticleParagraph.from(paragraph)),
                ArticleVisualBrief.from(visualBrief));
    }
    public ArticleSection content() { return content; }
    public ArticleVisualBrief visualBrief() { return visualBrief; }
    public GeneratedArticleSection withImage(ArticleImageRef image) { return new GeneratedArticleSection(content.withImage(image), visualBrief); }
}
