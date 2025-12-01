package com.nm.fragmentsclean.aticleContext.write.businesslogic.models;

import java.util.UUID;

public record ArticleId(UUID value) {
    public static ArticleId newId() {
        return new ArticleId(UUID.randomUUID());
    }
}
