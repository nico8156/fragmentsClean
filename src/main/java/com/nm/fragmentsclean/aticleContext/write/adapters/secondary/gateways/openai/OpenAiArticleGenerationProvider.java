package com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.openai;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways.ArticleGenerationProvider;

public final class OpenAiArticleGenerationProvider implements ArticleGenerationProvider {
    private final OpenAiArticleClient client;
    private final OpenAiArticleResponseV1Mapper mapper;
    private final OpenAiArticlePromptV1 prompt;

    OpenAiArticleGenerationProvider(OpenAiArticleClient client) {
        this.client = client; this.mapper = new OpenAiArticleResponseV1Mapper(); this.prompt = new OpenAiArticlePromptV1();
    }

    @Override public Result generate(Request request) {
        var response = client.generateArticle(OpenAiArticlePromptV1.INSTRUCTIONS, prompt.input(request));
        return new Result(mapper.map(response.body()), response.responseId(), response.model(), OpenAiArticleResponseV1Mapper.SCHEMA_VERSION);
    }
}
