package com.nm.fragmentsclean.aticleContext.read.projections;

public record ArticleBlockView(
        String heading,
        String paragraph,
        ImageRefView photo
) {
}
