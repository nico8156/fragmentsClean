package com.nm.fragmentsclean.articleContext.write.businesslogic.gateways;

import com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation.ArticleVisualBrief;

import java.util.UUID;

public interface ArticleImageGenerationProvider {
    GeneratedImage generate(Request request);
    enum Role { COVER, SECTION }
    record Request(UUID sagaId, UUID imageId, Role role, ArticleVisualBrief brief, String consistencyKey, com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation.ArticleArtDirection artDirection) {
        public Request(UUID sagaId, UUID imageId, Role role, ArticleVisualBrief brief, String consistencyKey) {
            this(sagaId, imageId, role, brief, consistencyKey, com.nm.fragmentsclean.articleContext.write.businesslogic.models.generation.ArticleArtDirection.ORIGINAL);
        }
        public Request { java.util.Objects.requireNonNull(artDirection, "artDirection"); }
    }
    record GeneratedImage(byte[] bytes, String mediaType, int width, int height, String model, String revisedPrompt) {
        public GeneratedImage { bytes = bytes.clone(); }
        @Override public byte[] bytes() { return bytes.clone(); }
    }
}
