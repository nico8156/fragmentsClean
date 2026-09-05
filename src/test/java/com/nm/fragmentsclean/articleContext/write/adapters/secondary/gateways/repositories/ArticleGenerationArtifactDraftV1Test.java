package com.nm.fragmentsclean.articleContext.write.adapters.secondary.gateways.repositories;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.ArticleImageRef;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation.ArticleEditorialTag;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation.GeneratedArticleDraft;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation.GeneratedArticleSection;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArticleGenerationArtifactDraftV1Test {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void round_trips_the_normalized_domain_draft_through_a_versioned_persistence_dto() throws Exception {
        var draft = GeneratedArticleDraft.from(
                "Comprendre les méthodes douces",
                "Une introduction claire.",
                "Une conclusion utile.",
                "Une cafetière et des grains sur une table lumineuse.",
                List.of(
                        section("Choisir", "Choisir son matériel."),
                        section("Moudre", "Adapter sa mouture."),
                        section("Extraire", "Maîtriser son extraction.")),
                List.of(ArticleEditorialTag.CULTURE_CAFE, ArticleEditorialTag.TUTO))
                .withGeneratedImages(
                        image("articles/cover.webp", 1024, 1536, "Couverture"),
                        List.of(
                                image("articles/section-1.webp", 1536, 1024, "Choisir"),
                                image("articles/section-2.webp", 1536, 1024, "Moudre"),
                                image("articles/section-3.webp", 1536, 1024, "Extraire")));

        var json = mapper.writeValueAsString(ArticleGenerationArtifactDraftV1.fromDomain(draft));
        var restored = mapper.readValue(json, ArticleGenerationArtifactDraftV1.class).toDomain();

        assertThat(json).contains("article-generation-artifact.v1");
        assertThat(restored.content().title().value()).isEqualTo("Comprendre les méthodes douces");
        assertThat(restored.coverImage().storageReference()).isEqualTo("articles/cover.webp");
        assertThat(restored.sections()).hasSize(3);
        assertThat(restored.sections().getFirst().content().images().getFirst().storageReference())
                .isEqualTo("articles/section-1.webp");
        assertThat(restored.tags()).containsExactly(ArticleEditorialTag.CULTURE_CAFE, ArticleEditorialTag.TUTO);
    }

    @Test
    void rejects_an_incompatible_persistence_schema() {
        assertThatThrownBy(() -> new ArticleGenerationArtifactDraftV1(
                "article-generation-artifact.v2", "Title", "Intro", "Conclusion", "Brief",
                new ArticleGenerationArtifactDraftV1.ImageV1("cover", 1024, 1536, "Cover"),
                List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported article generation artifact schema");
    }

    private static GeneratedArticleSection section(String heading, String paragraph) {
        return GeneratedArticleSection.from(heading, paragraph, "Illustrer " + heading);
    }

    private static ArticleImageRef image(String reference, int width, int height, String alt) {
        return ArticleImageRef.from(reference, width, height, alt);
    }
}
