package com.nm.fragmentsclean.socialContext.write.businesslogic.models;

import java.time.Instant;
import java.util.UUID;

public class Like {
    //Peut contenir : likeId, userId, targetId (ex: caféId, postId), createdAt.
    private final UUID likeId;
    private final UUID userId;
    private final UUID targetId;
    Instant createdAt;

    public Like(UUID likeId, UUID userId, UUID targetId) {
        this.likeId = likeId;
        this.userId = userId;
        this.targetId = targetId;
    }

    public record LikeSnapshot(UUID likeId, UUID userId, UUID targetId) {
    }
    public LikeSnapshot toSnapshot() {
        return new LikeSnapshot(this.likeId, this.userId, this.targetId);
    }
}
