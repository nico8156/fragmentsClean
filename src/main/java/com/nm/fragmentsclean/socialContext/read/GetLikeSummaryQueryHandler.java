package com.nm.fragmentsclean.socialContext.read;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;
import com.nm.fragmentsclean.socialContext.read.adapters.secondary.repositories.JdbcLikeProjectionRepository;
import com.nm.fragmentsclean.socialContext.read.projections.LikeSummaryView;

public class GetLikeSummaryQueryHandler implements QueryHandler<GetLikeSummaryQuery, LikeSummaryView> {
    private final JdbcLikeProjectionRepository projectionRepository;

    public GetLikeSummaryQueryHandler(JdbcLikeProjectionRepository projectionRepository) {
        this.projectionRepository = projectionRepository;
    }

    @Override
    public LikeSummaryView handle(GetLikeSummaryQuery query) {
        return projectionRepository.summaryFor(query.userId(), query.targetId());
    }
}
