package com.nm.fragmentsclean.articleContext.write.adapters.secondary.gateways.openai;

import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleImageGenerationProvider;

interface OpenAiArticleClient {
    ArticleResponse generateArticle(String instructions, String input);
    ImageResponse generateImage(String prompt, ArticleImageGenerationProvider.Role role);
    record ArticleResponse(OpenAiArticleResponseV1 body, String responseId, String model) {}
    record ImageResponse(byte[] bytes, String mediaType, int width, int height, String model, String revisedPrompt) {}
}
