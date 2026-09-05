package com.nm.fragmentsclean.articleContext.write.adapters.secondary.gateways.repositories;

import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleImageRef;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation.ArticleEditorialTag;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation.GeneratedArticleDraft;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation.GeneratedArticleSection;
import java.util.List;
import java.util.Objects;

/** Versioned persistence DTO; Jackson never serializes the rich domain model directly. */
record ArticleGenerationArtifactDraftV1(
        String schemaVersion,
        String title,
        String introduction,
        String conclusion,
        String coverVisualBrief,
        ImageV1 coverImage,
        List<SectionV1> sections,
        List<String> tags
) {
    static final String SCHEMA_VERSION = "article-generation-artifact.v1";

    ArticleGenerationArtifactDraftV1 {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException("Unsupported article generation artifact schema: " + schemaVersion);
        }
        sections = List.copyOf(Objects.requireNonNull(sections, "sections are required"));
        tags = List.copyOf(Objects.requireNonNull(tags, "tags are required"));
    }

    static ArticleGenerationArtifactDraftV1 fromDomain(GeneratedArticleDraft draft) {
        Objects.requireNonNull(draft, "draft is required");
        return new ArticleGenerationArtifactDraftV1(
                SCHEMA_VERSION,
                draft.content().title().value(),
                draft.content().introduction().value(),
                draft.content().conclusion().value(),
                draft.coverVisualBrief().value(),
                ImageV1.fromDomain(requireImage(draft.coverImage(), "cover")),
                draft.sections().stream().map(SectionV1::fromDomain).toList(),
                draft.tags().stream().map(ArticleEditorialTag::label).toList());
    }

    GeneratedArticleDraft toDomain() {
        var generatedSections = sections.stream().map(SectionV1::toDomain).toList();
        var draft = GeneratedArticleDraft.from(
                title,
                introduction,
                conclusion,
                coverVisualBrief,
                generatedSections,
                tags.stream().map(ArticleEditorialTag::fromProvider).toList());
        return draft.withGeneratedImages(
                coverImage.toDomain(),
                sections.stream().map(section -> section.image().toDomain()).toList());
    }

    private static ArticleImageRef requireImage(ArticleImageRef image, String slot) {
        if (image == null) {
            throw new IllegalStateException("Generated article artifact is missing " + slot + " image");
        }
        return image;
    }

    record SectionV1(String heading, String paragraph, String visualBrief, ImageV1 image) {
        SectionV1 {
            Objects.requireNonNull(image, "section image is required");
        }

        static SectionV1 fromDomain(GeneratedArticleSection section) {
            var paragraphs = section.content().paragraphs();
            if (paragraphs.size() != 1) {
                throw new IllegalStateException("Generated article artifact requires exactly one paragraph per section");
            }
            var images = section.content().images();
            if (images.size() != 1) {
                throw new IllegalStateException("Generated article artifact requires exactly one image per section");
            }
            return new SectionV1(
                    section.content().heading(),
                    paragraphs.getFirst().value(),
                    section.visualBrief().value(),
                    ImageV1.fromDomain(images.getFirst()));
        }

        GeneratedArticleSection toDomain() {
            return GeneratedArticleSection.from(heading, paragraph, visualBrief);
        }
    }

    record ImageV1(String storageReference, int width, int height, String alt) {
        static ImageV1 fromDomain(ArticleImageRef image) {
            return new ImageV1(image.storageReference(), image.width(), image.height(), image.alt());
        }

        ArticleImageRef toDomain() {
            return ArticleImageRef.from(storageReference, width, height, alt);
        }
    }
}
