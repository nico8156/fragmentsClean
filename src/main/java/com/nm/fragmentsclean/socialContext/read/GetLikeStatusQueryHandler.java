package com.nm.fragmentsclean.socialContext.read;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.CurrentUserProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.query.QueryHandler;
import com.nm.fragmentsclean.socialContext.read.projections.GetLikeStatusQuery;
import com.nm.fragmentsclean.socialContext.read.projections.LikeStatusView;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.SpringLikeRepository;
import com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.entities.LikeJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class GetLikeStatusQueryHandler implements QueryHandler<GetLikeStatusQuery, LikeStatusView> {

    private final SpringLikeRepository likeRepository;
    private final CurrentUserProvider currentUserProvider;
    private final DateTimeProvider dateTimeProvider;

    public GetLikeStatusQueryHandler(SpringLikeRepository likeRepository,
                                     CurrentUserProvider currentUserProvider,
                                     DateTimeProvider dateTimeProvider) {
        this.likeRepository = likeRepository;
        this.currentUserProvider = currentUserProvider;
        this.dateTimeProvider = dateTimeProvider;
    }

    @Override
    public LikeStatusView handle(GetLikeStatusQuery query) {
        var targetId = query.targetId();

        long count = likeRepository.countByTargetIdAndActiveTrue(targetId);

        var meId = currentUserProvider.currentUserId();
        boolean me = likeRepository.existsByTargetIdAndUserIdAndActiveTrue(targetId, meId);

        var likes = likeRepository.findByTargetId(targetId);
        long version = likes.stream()
                .mapToLong(LikeJpaEntity::getVersion)
                .max()
                .orElse(0L);

        String serverTime = dateTimeProvider.now().toString();

        return new LikeStatusView(count, me, version, serverTime);
    }
}
