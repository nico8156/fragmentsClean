package com.nm.fragmentsclean.socialContext.read;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.CurrentUserProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;
import com.nm.fragmentsclean.socialContext.read.adapters.secondary.repositories.JdbcLikeProjectionRepository;
import com.nm.fragmentsclean.socialContext.read.projections.GetLikeStatusQuery;
import com.nm.fragmentsclean.socialContext.read.projections.LikeStatusView;
import org.springframework.stereotype.Component;

@Component
public class GetLikeStatusQueryHandler implements QueryHandler<GetLikeStatusQuery, LikeStatusView> {

    private final JdbcLikeProjectionRepository projectionRepository;
    private final CurrentUserProvider currentUserProvider;
    private final DateTimeProvider dateTimeProvider;

    public GetLikeStatusQueryHandler(JdbcLikeProjectionRepository projectionRepository,
                                     CurrentUserProvider currentUserProvider,
                                     DateTimeProvider dateTimeProvider) {
        this.projectionRepository = projectionRepository;
        this.currentUserProvider = currentUserProvider;
        this.dateTimeProvider = dateTimeProvider;
    }

    @Override
    public LikeStatusView handle(GetLikeStatusQuery query) {
        return projectionRepository.statusFor(
                query.targetId(),
                currentUserProvider.currentUserId(),
                dateTimeProvider.now());
    }
}
