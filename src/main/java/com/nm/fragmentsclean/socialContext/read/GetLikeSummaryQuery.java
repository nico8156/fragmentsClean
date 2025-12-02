package com.nm.fragmentsclean.socialContext.read;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.Query;
import com.nm.fragmentsclean.socialContext.read.projections.LikeSummaryView;

import java.util.UUID;

public record GetLikeSummaryQuery(
        UUID userId,
        UUID targetId
)implements Query<LikeSummaryView> {
}
