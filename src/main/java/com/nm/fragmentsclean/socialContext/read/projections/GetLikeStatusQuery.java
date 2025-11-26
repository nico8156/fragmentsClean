package com.nm.fragmentsclean.socialContext.read.projections;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.Query;

import java.util.UUID;

public record GetLikeStatusQuery(
        UUID targetId
)implements Query<LikeStatusView> {
}
