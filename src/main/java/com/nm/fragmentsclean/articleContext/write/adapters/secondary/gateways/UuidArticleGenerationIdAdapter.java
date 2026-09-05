package com.nm.fragmentsclean.articleContext.write.adapters.secondary.gateways;

import com.nm.fragmentsclean.articleContext.write.businesslogic.gateways.ArticleGenerationIdPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public final class UuidArticleGenerationIdAdapter implements ArticleGenerationIdPort {
    @Override
    public UUID next() {
        return UUID.randomUUID();
    }
}
