package com.nm.fragmentsclean.articleContext.write.adapters.secondary.gateways.openai;

import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleGenerationProvider;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleGenerationProviderException;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleImageGenerationProvider;
import com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation.ArticleSubject;
import org.junit.jupiter.api.Test;
import com.openai.core.JsonSchemaLocalValidation;
import com.openai.models.responses.ResponseCreateParams;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OpenAiArticleGenerationProviderTest {
    @Test void openAiSdkCanBuildTheStrictVersionedSchemaLocally() {
        var params = ResponseCreateParams.builder()
                .text(OpenAiArticleResponseV1.class, JsonSchemaLocalValidation.YES)
                .model("gpt-4o-mini").instructions(OpenAiArticlePromptV1.INSTRUCTIONS)
                .input("Sujet éditorial: test").store(false).build();
        assertEquals(OpenAiArticleResponseV1.class, params.responseType());
    }

    @Test void mapsTheVersionedTechnicalDtoThroughTheAcl() {
        var client = new FakeClient(validResponse());
        var result = new OpenAiArticleGenerationProvider(client).generate(
                new ArticleGenerationProvider.Request(UUID.randomUUID(), ArticleSubject.from("Les méthodes douces"), "fr-FR"));
        assertEquals("article-generation.v1", result.schemaVersion());
        assertEquals(3, result.draft().sections().size());
        assertTrue(client.input.contains("Les méthodes douces"));
        assertFalse(client.instructions.contains("Les méthodes douces"));
    }

    @Test void rejectsAnIncompatibleProviderSchema() {
        var dto = validResponse(); dto.schemaVersion = "article-generation.v2";
        assertThrows(ArticleGenerationProviderException.class, () -> new OpenAiArticleGenerationProvider(new FakeClient(dto)).generate(
                new ArticleGenerationProvider.Request(UUID.randomUUID(), ArticleSubject.from("Sujet"), "fr-FR")));
    }

    @Test void buildsOnePortraitCoverAndLandscapeSectionImage() {
        var client = new FakeClient(validResponse());
        var provider = new OpenAiArticleImageGenerationProvider(client);
        var cover = provider.generate(new ArticleImageGenerationProvider.Request(UUID.randomUUID(), UUID.randomUUID(),
                ArticleImageGenerationProvider.Role.COVER, com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation.ArticleVisualBrief.from("Tasses empilées"), "article-1"));
        assertEquals(1024, cover.width()); assertEquals(1536, cover.height());
        assertTrue(client.imagePrompt.contains("Aucun texte visible"));
    }

    @Test void contemplativeDirectionOverridesWarmBriefWithoutChangingTheSubject() {
        var client = new FakeClient(validResponse());
        new OpenAiArticleImageGenerationProvider(client).generate(new ArticleImageGenerationProvider.Request(
                UUID.randomUUID(), UUID.randomUUID(), ArticleImageGenerationProvider.Role.COVER,
                com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation.ArticleVisualBrief.from("Tasses empilées, palette chaude"),
                "article-1", com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation.ArticleArtDirection.CONTEMPLATIVE));
        assertTrue(client.imagePrompt.contains("sauge, bleu grisé, ivoire"));
        assertTrue(client.imagePrompt.contains("lumière diffuse"));
        assertTrue(client.imagePrompt.contains("Tasses empilées"));
        assertTrue(client.imagePrompt.contains("prioritaire"));
        assertTrue(client.imagePrompt.contains("Aucun texte visible"));
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({
        "INTIMATE, 'crème, terre cuite, brun café', cadrages proches",
        "LIVELY, 'corail, jaune safran, bleu', diagonales",
        "CONTEMPLATIVE, 'sauge, bleu grisé, ivoire', compositions aérées",
        "BOLD, 'prune, orange brûlé, rose', cadrages inattendus"
    })
    void eachDirectionHasItsOwnPaletteAndComposition(String name, String palette, String composition) {
        var client = new FakeClient(validResponse());
        new OpenAiArticleImageGenerationProvider(client).generate(new ArticleImageGenerationProvider.Request(
                UUID.randomUUID(), UUID.randomUUID(), ArticleImageGenerationProvider.Role.SECTION,
                com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation.ArticleVisualBrief.from("Une tasse"), "article-1",
                com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation.ArticleArtDirection.valueOf(name)));
        assertTrue(client.imagePrompt.contains(palette));
        assertTrue(client.imagePrompt.contains(composition));
    }

    @Test void theDefaultDirectionPreservesTheHistoricalPromptExactly() {
        var client = new FakeClient(validResponse());
        new OpenAiArticleImageGenerationProvider(client).generate(new ArticleImageGenerationProvider.Request(
                UUID.randomUUID(), UUID.randomUUID(), ArticleImageGenerationProvider.Role.COVER,
                com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation.ArticleVisualBrief.from("Une tasse"), "article-1"));
        assertEquals("""
                Illustration éditoriale contemporaine sur le café, dessinée et légèrement texturée, palette chaude et colorée,
                ludique mais adulte, composition claire pour application mobile. Cohérence de collection: article-1.
                Aucun texte visible, aucun logo, aucune marque, aucune personne reconnaissable. Sujet: Une tasse
                """, client.imagePrompt);
    }

    private static OpenAiArticleResponseV1 validResponse() {
        var dto = new OpenAiArticleResponseV1(); dto.schemaVersion = "article-generation.v1";
        dto.title = "Les méthodes douces"; dto.introduction = "Une introduction"; dto.conclusion = "Une conclusion";
        dto.coverVisualBrief = "Une couverture"; dto.tags = List.of("culture cafe", "decouverte");
        dto.sections = List.of(section("Choisir", "Choisir son matériel"), section("Moudre", "Adapter sa mouture"), section("Extraire", "Maîtriser son extraction"));
        return dto;
    }
    private static OpenAiArticleResponseV1.Section section(String heading, String paragraph) {
        var section = new OpenAiArticleResponseV1.Section(); section.heading = heading; section.paragraph = paragraph; section.visualBrief = "Illustrer " + heading; return section;
    }

    private static final class FakeClient implements OpenAiArticleClient {
        private final OpenAiArticleResponseV1 response; String instructions; String input; String imagePrompt;
        private FakeClient(OpenAiArticleResponseV1 response) { this.response = response; }
        @Override public ArticleResponse generateArticle(String instructions, String input) { this.instructions = instructions; this.input = input; return new ArticleResponse(response, "resp-1", "gpt-4o-mini"); }
        @Override public ImageResponse generateImage(String prompt, ArticleImageGenerationProvider.Role role) { this.imagePrompt = prompt; boolean cover = role == ArticleImageGenerationProvider.Role.COVER; return new ImageResponse("image".getBytes(StandardCharsets.UTF_8), "image/webp", cover ? 1024 : 1536, cover ? 1536 : 1024, "gpt-image-1-mini", null); }
    }
}
