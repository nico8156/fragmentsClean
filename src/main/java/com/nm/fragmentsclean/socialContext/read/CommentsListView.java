package com.nm.fragmentsclean.socialContext.read;

import com.nm.fragmentsclean.socialContext.read.projections.CommentView;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CommentsListView(
        UUID targetId,
        String op,              // "retrieve" | "older" | "refresh" si tu veux coller au front
        List<CommentView> items,
        String nextCursor,
        String prevCursor,
        Instant serverTime
) {
}
