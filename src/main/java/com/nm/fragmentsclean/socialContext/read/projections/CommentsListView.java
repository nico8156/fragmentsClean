package com.nm.fragmentsclean.socialContext.read.projections;

import java.time.Instant;
import java.util.UUID;

public record CommentsListView(
        UUID targetId,
        String op,
        java.util.List<CommentItemView> items,
        String nextCursor,
        String prevCursor,
        Instant serverTime
) {}
