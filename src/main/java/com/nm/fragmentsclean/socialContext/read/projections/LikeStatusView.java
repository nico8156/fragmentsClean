package com.nm.fragmentsclean.socialContext.read.projections;

public record LikeStatusView(
        long count,
        boolean me,
        long version,
        String serverTime // ISO-8601, ou null si tu veux le rendre vraiment optionnel
) {
}
