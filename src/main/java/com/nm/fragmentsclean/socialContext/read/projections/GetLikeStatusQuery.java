package com.nm.fragmentsclean.socialContext.read.projections;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.Query;

import java.util.UUID;

public record GetLikeStatusQuery(
        UUID targetId
)implements Query<LikeStatusView> {
}
