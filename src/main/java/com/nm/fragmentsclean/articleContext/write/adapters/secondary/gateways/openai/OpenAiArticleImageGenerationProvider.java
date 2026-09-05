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
        var response = client.generateImage(ART_DIRECTION.formatted(request.consistencyKey(), request.brief().value()), request.role());
        return new GeneratedImage(response.bytes(), response.mediaType(), response.width(), response.height(), response.model(), response.revisedPrompt());
    }
}
