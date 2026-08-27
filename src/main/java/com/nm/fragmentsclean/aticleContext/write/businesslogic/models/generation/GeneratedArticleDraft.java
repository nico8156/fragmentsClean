package com.nm.fragmentsclean.aticleContext.write.businesslogic.models.generation;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.*;

import java.util.List;
import java.util.Objects;

public final class GeneratedArticleDraft {
    private final ArticleContent content;
    private final ArticleVisualBrief coverVisualBrief;
    private final List<GeneratedArticleSection> sections;
    private final List<ArticleEditorialTag> tags;

    private GeneratedArticleDraft(ArticleContent content, ArticleVisualBrief coverVisualBrief,
                                  List<GeneratedArticleSection> sections, List<ArticleEditorialTag> tags) {
        this.content = content; this.coverVisualBrief = coverVisualBrief;
        this.sections = List.copyOf(sections); this.tags = List.copyOf(tags);
    }

    public static GeneratedArticleDraft from(String title, String introduction, String conclusion,
                                             String coverVisualBrief, List<GeneratedArticleSection> sections,
                                             List<ArticleEditorialTag> tags) {
        var safeSections = List.copyOf(Objects.requireNonNull(sections));
        var content = ArticleContent.draft(ArticleTitle.from(title), ArticleIntroduction.from(introduction),
                safeSections.stream().map(GeneratedArticleSection::content).toList(), ArticleParagraph.from(conclusion));
        var draft = new GeneratedArticleDraft(content, ArticleVisualBrief.from(coverVisualBrief), safeSections, tags);
        ArticleEditorialPolicy.validate(draft);
        return draft;
    }
    public ArticleContent content() { return content; }
    public ArticleVisualBrief coverVisualBrief() { return coverVisualBrief; }
    public List<GeneratedArticleSection> sections() { return sections; }
    public List<ArticleEditorialTag> tags() { return tags; }
}
