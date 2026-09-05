package com.nm.fragmentsclean.articleContext.read.projections;

public record ImageRefView(
        String url,
        Integer width,
        Integer height,
        String alt
) {
}
