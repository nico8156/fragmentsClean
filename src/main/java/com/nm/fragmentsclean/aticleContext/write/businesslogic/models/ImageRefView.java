package com.nm.fragmentsclean.aticleContext.write.businesslogic.models;

public record ImageRefView(
        String url,
        Integer width,
        Integer height,
        String alt   // nullable = alt?: string
) {
}
