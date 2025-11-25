package com.nm.fragmentsclean.socialContext.write.adapters; // (typo adapter plus tard)

import com.nm.fragmentsclean.socialContext.write.businesslogic.gateways.LikeRepository;
import com.nm.fragmentsclean.socialContext.write.businesslogic.models.Like;

import java.util.*;

public class FakeLikeRepository implements LikeRepository {

    // source de vérité en mémoire : snapshot par likeId
    public final Map<UUID, Like.LikeSnapshot> snapshots = new HashMap<>();

    @Override
    public Optional<Like> byId(UUID likeId) {
        return Optional.ofNullable(snapshots.get(likeId))
                .map(Like::fromSnapshot);
    }

    @Override
    public Optional<Like> byUserIdAndTargetId(UUID userId, UUID targetId) {
        return snapshots.values().stream()
                .filter(s -> s.userId().equals(userId) && s.targetId().equals(targetId))
                .findFirst()
                .map(Like::fromSnapshot);
    }

    @Override
    public void save(Like like) {
        snapshots.put(like.toSnapshot().likeId(), like.toSnapshot());
    }
    @Override
    public long countByTargetId(UUID targetId) {
        return snapshots.values().stream()
                .filter(Like.LikeSnapshot::active)
                .filter(s -> s.targetId().equals(targetId))
                .count();
    }
}
