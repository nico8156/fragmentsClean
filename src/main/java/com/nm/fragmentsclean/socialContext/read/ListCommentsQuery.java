package com.nm.fragmentsclean.socialContext.read;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.Query;
import com.nm.fragmentsclean.socialContext.read.projections.CommentsListView;

import java.util.UUID;

public record ListCommentsQuery(
        UUID targetId,
        String cursor,
        int limit,
        String op // "retrieve" | "older" | "refresh"
) implements Query<CommentsListView> {
}
