package com.nm.fragmentsclean.aticleContext.read.projections;

public record ImageRefView(
        String url,
        Integer width,
        Integer height,
        String alt
) {
}
