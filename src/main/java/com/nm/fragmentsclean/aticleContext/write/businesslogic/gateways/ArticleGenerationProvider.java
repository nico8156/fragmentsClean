package com.nm.fragmentsclean.aticleContext.write.businesslogic.gateways;

import com.nm.fragmentsclean.aticleContext.write.businesslogic.models.generation.*;

import java.util.UUID;

public interface ArticleGenerationProvider {
    Result generate(Request request);

    record Request(UUID sagaId, ArticleSubject subject, String locale) {
        public Request { if (sagaId == null || subject == null || locale == null || locale.isBlank()) throw new IllegalArgumentException("Invalid generation request"); }
    }
    record Result(GeneratedArticleDraft draft, String providerResponseId, String model, String schemaVersion) {}
}
