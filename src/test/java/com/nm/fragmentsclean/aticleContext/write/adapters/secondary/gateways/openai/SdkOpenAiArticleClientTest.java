package com.nm.fragmentsclean.aticleContext.write.adapters.secondary.gateways.openai;

import com.openai.models.ChatModel;
import com.openai.models.ResponsesModel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SdkOpenAiArticleClientTest {

    @Test
    void reads_every_model_variant_exposed_by_the_responses_sdk() {
        assertThat(SdkOpenAiArticleClient.modelName(ResponsesModel.ofString("custom-model"), "requested-model"))
                .isEqualTo("custom-model");
        assertThat(SdkOpenAiArticleClient.modelName(ResponsesModel.ofChat(ChatModel.GPT_4O_MINI), "requested-model"))
                .isEqualTo("gpt-4o-mini");
        assertThat(SdkOpenAiArticleClient.modelName(
                ResponsesModel.ofOnly(ResponsesModel.ResponsesOnlyModel.O1_PRO), "requested-model"))
                .isEqualTo("o1-pro");
    }
}
