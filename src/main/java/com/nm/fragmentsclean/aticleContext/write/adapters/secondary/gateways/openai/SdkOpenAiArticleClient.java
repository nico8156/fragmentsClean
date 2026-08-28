package com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.openai;

import com.openai.client.OpenAIClient;
import com.openai.core.JsonSchemaLocalValidation;
import com.openai.errors.OpenAIException;
import com.openai.errors.OpenAIRetryableException;
import com.openai.models.images.ImageGenerateParams;
import com.openai.models.responses.ResponseCreateParams;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.ArticleGenerationProviderException;
import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.ArticleImageGenerationProvider;

import java.util.Base64;

final class SdkOpenAiArticleClient implements OpenAiArticleClient {
    private final OpenAIClient client;
    private final String textModel;
    private final String imageModel;

    SdkOpenAiArticleClient(OpenAIClient client, String textModel, String imageModel) {
        this.client = client; this.textModel = textModel; this.imageModel = imageModel;
    }

    @Override public ArticleResponse generateArticle(String instructions, String input) {
        try {
            var params = ResponseCreateParams.builder()
                    .text(OpenAiArticleResponseV1.class, JsonSchemaLocalValidation.YES)
                    .model(textModel).instructions(instructions).input(input)
                    .temperature(0.2).maxOutputTokens(5_000).store(false).build();
            var response = client.responses().create(params);
            OpenAiArticleResponseV1 body = response.output().stream()
                    .flatMap(item -> item.message().stream())
                    .flatMap(message -> message.content().stream())
                    .flatMap(content -> content.outputText().stream())
                    .findFirst()
                    .orElseThrow(() -> new ArticleGenerationProviderException("OpenAI returned no structured article", false));
            return new ArticleResponse(body, response.id(), response.model().asString());
        } catch (ArticleGenerationProviderException error) { throw error; }
        catch (OpenAIException error) { throw providerFailure("OpenAI article generation failed", error); }
    }

    @Override public ImageResponse generateImage(String prompt, ArticleImageGenerationProvider.Role role) {
        try {
            var portrait = role == ArticleImageGenerationProvider.Role.COVER;
            var params = ImageGenerateParams.builder().model(imageModel).prompt(prompt).n(1)
                    .quality(ImageGenerateParams.Quality.MEDIUM)
                    .size(portrait ? ImageGenerateParams.Size._1024X1536 : ImageGenerateParams.Size._1536X1024)
                    .outputFormat(ImageGenerateParams.OutputFormat.WEBP).build();
            var response = client.images().generate(params);
            var image = response.data().flatMap(images -> images.stream().findFirst())
                    .orElseThrow(() -> new ArticleGenerationProviderException("OpenAI returned no image", false));
            String encoded = image.b64Json().orElseThrow(() -> new ArticleGenerationProviderException("OpenAI returned no image bytes", false));
            return new ImageResponse(Base64.getDecoder().decode(encoded), "image/webp",
                    portrait ? 1024 : 1536, portrait ? 1536 : 1024, imageModel, image.revisedPrompt().orElse(null));
        } catch (ArticleGenerationProviderException error) { throw error; }
        catch (OpenAIException | IllegalArgumentException error) { throw providerFailure("OpenAI image generation failed", error); }
    }

    private ArticleGenerationProviderException providerFailure(String message, RuntimeException error) {
        return new ArticleGenerationProviderException(message, error instanceof OpenAIRetryableException, error);
    }
}
