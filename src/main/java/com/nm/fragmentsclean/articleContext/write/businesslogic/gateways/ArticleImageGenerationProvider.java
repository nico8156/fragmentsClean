package com.nm.fragmentsclean.articleContext.write.businesslogic.gateways;

import com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation.ArticleVisualBrief;

import java.util.UUID;

public interface ArticleImageGenerationProvider {
    GeneratedImage generate(Request request);
    enum Role { COVER, SECTION }
    record Request(UUID sagaId, UUID imageId, Role role, ArticleVisualBrief brief, String consistencyKey) {}
    record GeneratedImage(byte[] bytes, String mediaType, int width, int height, String model, String revisedPrompt) {
        public GeneratedImage { bytes = bytes.clone(); }
        @Override public byte[] bytes() { return bytes.clone(); }
    }
}
