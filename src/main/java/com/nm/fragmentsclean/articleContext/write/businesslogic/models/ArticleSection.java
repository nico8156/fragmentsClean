package com.nm.fragmentsclean.articleContext.write.businesslogic.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ArticleSection {

    private static final int MAX_TITLE_LENGTH = 140;
    private final String heading;
    private final List<ArticleParagraph> paragraphs;
    private final List<ArticleImageRef> images;

    private ArticleSection(String heading,
                           List<ArticleParagraph> paragraphs,
                           List<ArticleImageRef> images) {
        this.heading = heading;
        this.paragraphs = new ArrayList<>(paragraphs);
        this.images = new ArrayList<>(images);
    }

    public static ArticleSection draft(String heading) {
        var value = Objects.requireNonNull(heading, "Le titre de section est obligatoire.").trim();
        if (value.isEmpty() || value.length() > MAX_TITLE_LENGTH) {
            throw new ArticleDomainException("Le titre de section est invalide.");
        }
        return new ArticleSection(value, List.of(), List.of());
    }

    public ArticleSection withParagraph(ArticleParagraph paragraph) {
        var next = new ArrayList<>(paragraphs);
        next.add(Objects.requireNonNull(paragraph, "Le paragraphe est obligatoire."));
        return new ArticleSection(heading, next, images);
    }

    public ArticleSection withImage(ArticleImageRef image) {
        var next = new ArrayList<>(images);
        next.add(Objects.requireNonNull(image, "L'image est obligatoire."));
        return new ArticleSection(heading, paragraphs, next);
    }

    public String heading() {
        return heading;
    }

    public List<ArticleParagraph> paragraphs() {
        return List.copyOf(paragraphs);
    }

    public List<ArticleImageRef> images() {
        return List.copyOf(images);
    }

    public boolean hasContent() {
        return !paragraphs.isEmpty();
    }
}
