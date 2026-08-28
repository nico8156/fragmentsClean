package com.nm.fragmentsclean.aticleContext.write.businesslogic.models.generation;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.*;

import java.util.List;
import java.util.Objects;

public final class GeneratedArticleDraft {
    private final ArticleContent content;
    private final ArticleVisualBrief coverVisualBrief;
    private final ArticleImageRef coverImage;
    private final List<GeneratedArticleSection> sections;
    private final List<ArticleEditorialTag> tags;

    private GeneratedArticleDraft(ArticleContent content, ArticleVisualBrief coverVisualBrief, ArticleImageRef coverImage,
                                  List<GeneratedArticleSection> sections, List<ArticleEditorialTag> tags) {
        this.content = content; this.coverVisualBrief = coverVisualBrief;
        this.coverImage = coverImage;
        this.sections = List.copyOf(sections); this.tags = List.copyOf(tags);
    }

    public static GeneratedArticleDraft from(String title, String introduction, String conclusion,
                                             String coverVisualBrief, List<GeneratedArticleSection> sections,
                                             List<ArticleEditorialTag> tags) {
        var safeSections = List.copyOf(Objects.requireNonNull(sections));
        var content = ArticleContent.draft(ArticleTitle.from(title), ArticleIntroduction.from(introduction),
                safeSections.stream().map(GeneratedArticleSection::content).toList(), ArticleParagraph.from(conclusion));
        var draft = new GeneratedArticleDraft(content, ArticleVisualBrief.from(coverVisualBrief), null, safeSections, tags);
        ArticleEditorialPolicy.validate(draft);
        return draft;
    }
    public ArticleContent content() { return content; }
    public ArticleVisualBrief coverVisualBrief() { return coverVisualBrief; }
    public ArticleImageRef coverImage() { return coverImage; }
    public List<GeneratedArticleSection> sections() { return sections; }
    public List<ArticleEditorialTag> tags() { return tags; }
    public GeneratedArticleDraft withGeneratedImages(ArticleImageRef cover, List<ArticleImageRef> sectionImages) {
        Objects.requireNonNull(cover,"cover"); var images=List.copyOf(sectionImages);
        if(images.size()!=sections.size()) throw new ArticleDomainException("Une image est requise par section.");
        var enriched=new java.util.ArrayList<GeneratedArticleSection>();
        for(int i=0;i<sections.size();i++) enriched.add(sections.get(i).withImage(images.get(i)));
        var enrichedContent=ArticleContent.draft(content.title(),content.introduction(),enriched.stream().map(GeneratedArticleSection::content).toList(),content.conclusion());
        return new GeneratedArticleDraft(enrichedContent,coverVisualBrief,cover,enriched,tags);
    }
}
