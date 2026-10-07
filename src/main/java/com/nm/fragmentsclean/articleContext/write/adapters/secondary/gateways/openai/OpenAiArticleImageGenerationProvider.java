package com.nm.fragmentsclean.articleContext.write.adapters.secondary.gateways.openai;

import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleImageGenerationProvider;

public final class OpenAiArticleImageGenerationProvider implements ArticleImageGenerationProvider {
    private static final String ART_DIRECTION = """
            Illustration éditoriale contemporaine sur le café, dessinée et légèrement texturée, palette chaude et colorée,
            ludique mais adulte, composition claire pour application mobile. Cohérence de collection: %s.
            Aucun texte visible, aucun logo, aucune marque, aucune personne reconnaissable. Sujet: %s
            """;
    private final OpenAiArticleClient client;
    OpenAiArticleImageGenerationProvider(OpenAiArticleClient client) { this.client = client; }

    @Override public GeneratedImage generate(Request request) {
        String prompt = ART_DIRECTION.formatted(request.consistencyKey(), request.brief().value());
        if (request.artDirection() != com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation.ArticleArtDirection.ORIGINAL) {
            prompt = """
                    Illustration éditoriale contemporaine sur le café, dessinée et légèrement texturée, ludique mais adulte.
                    Composition lisible pour application mobile. Cohérence de collection: %s.
                    Direction artistique prioritaire sur les indications de style et de couleur du sujet: %s.
                    Aucun texte visible, aucun logo, aucune marque, aucune personne reconnaissable. Sujet: %s
                    """.formatted(request.consistencyKey(), direction(request.artDirection()), request.brief().value());
        }
        var response = client.generateImage(prompt, request.role());
        return new GeneratedImage(response.bytes(), response.mediaType(), response.width(), response.height(), response.model(), response.revisedPrompt());
    }
    private static String direction(com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation.ArticleArtDirection direction) {
        return switch (direction) {
            case ORIGINAL -> throw new IllegalArgumentException("Original uses the historical prompt");
            case INTIMATE -> "humeur intime et chaleureuse; palette crème, terre cuite, brun café; lumière douce, cadrages proches, textures tactiles";
            case LIVELY -> "humeur vivante et joyeuse; palette corail, jaune safran, bleu; lumière franche, compositions dynamiques et diagonales, contrastes francs";
            case CONTEMPLATIVE -> "humeur contemplative et paisible; palette sauge, bleu grisé, ivoire; lumière diffuse, compositions aérées, espace et respiration";
            case BOLD -> "humeur audacieuse et expérimentale; palette prune, orange brûlé, rose; lumière contrastée, cadrages inattendus, formes graphiques affirmées";
        };
    }
}
