package com.nm.fragmentsclean.articleContext.write.adapters.secondary.gateways.openai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleGenerationProvider;
import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleImageGenerationProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "fragments.article.generation.openai.enabled", havingValue = "true")
public class OpenAiArticleGenerationConfiguration {
    @Bean(destroyMethod = "close") OpenAIClient articleOpenAiClient() { return OpenAIOkHttpClient.fromEnv(); }
    @Bean OpenAiArticleClient openAiArticleClient(OpenAIClient client,
                                                  @Value("${fragments.article.generation.openai.text-model:gpt-4o-mini}") String textModel,
                                                  @Value("${fragments.article.generation.openai.image-model:gpt-image-1-mini}") String imageModel) {
        return new SdkOpenAiArticleClient(client, textModel, imageModel);
    }
    @Bean ArticleGenerationProvider articleGenerationProvider(OpenAiArticleClient client) { return new OpenAiArticleGenerationProvider(client); }
    @Bean ArticleImageGenerationProvider articleImageGenerationProvider(OpenAiArticleClient client) { return new OpenAiArticleImageGenerationProvider(client); }
}
