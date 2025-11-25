package com.nm.fragmentsclean.socialContext.write.businesslogic.gateways;

import com.nm.fragmentsclean.socialContext.write.businesslogic.models.Like;

import java.util.Optional;
import java.util.UUID;

public interface LikeRepository {

    Optional<Like> byId(UUID likeId);

    Optional<Like> byUserIdAndTargetId(UUID userId, UUID targetId);

    void save(Like like);
}
