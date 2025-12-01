package com.nm.fragmentsclean.aticleContext.write.businesslogic.models;

public record ArticleBlockView(
        String heading,
        String paragraph,
        ImageRefView photo // nullable = photo?: ImageRef
) {
}
