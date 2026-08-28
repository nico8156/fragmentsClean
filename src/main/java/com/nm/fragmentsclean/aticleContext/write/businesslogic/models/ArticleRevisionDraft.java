package com.nm.fragmentsclean.aticleContext.write.businesslogic.models;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.generation.ArticleEditorialTag;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/** Complete editable content owned by an article revision. */
public final class ArticleRevisionDraft {
    private final ArticleContent content;
    private final ArticleImageRef cover;
    private final List<ArticleEditorialTag> tags;

    private ArticleRevisionDraft(ArticleContent content, ArticleImageRef cover,
                                 List<ArticleEditorialTag> tags) {
        this.content = Objects.requireNonNull(content, "Le contenu est obligatoire.");
        this.cover = cover;
        this.tags = List.copyOf(Objects.requireNonNull(tags, "Les tags sont obligatoires."));
        if (this.tags.size() > 3 || new HashSet<>(this.tags).size() != this.tags.size()) {
            throw new ArticleDomainException("Un article contient au maximum 3 tags distincts.");
        }
    }

    public static ArticleRevisionDraft editable(ArticleContent content, ArticleImageRef cover,
                                                 List<ArticleEditorialTag> tags) {
        return new ArticleRevisionDraft(content, cover, tags);
    }

    public void validateForReview() {
        content.validateForReview();
        if (cover == null) {
            throw new ArticleDomainException("La couverture est obligatoire avant revue.");
        }
        if (tags.isEmpty()) {
            throw new ArticleDomainException("Au moins un tag est obligatoire avant revue.");
        }
    }

    public ArticleContent content() { return content; }
    public ArticleImageRef cover() { return cover; }
    public List<ArticleEditorialTag> tags() { return tags; }
}
