package com.nm.fragmentsclean.socialContext.read.projections;

import java.util.UUID;

public record LikeSummaryView(
        UUID userId,
        UUID targetId,
        boolean active,
        long likeCount
) {
}
