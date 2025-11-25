package com.nm.fragmentsclean.socialContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.AggregateRoot;

import java.time.Instant;
import java.util.UUID;

public class Like extends AggregateRoot {

    private final UUID userId;
    private final UUID targetId;
    private boolean active;
    private Instant updatedAt;

    private Like(UUID likeId,
                 UUID userId,
                 UUID targetId,
                 boolean active,
                 Instant updatedAt) {
        super(likeId);
        this.userId = userId;
        this.targetId = targetId;
        this.active = active;
        this.updatedAt = updatedAt;
    }

    public static Like createNew(UUID likeId,
                                 UUID userId,
                                 UUID targetId,
                                 Instant now) {
        return new Like(likeId, userId, targetId, false, now);
    }

    public static Like fromSnapshot(LikeSnapshot snapshot) {
        return new Like(
                snapshot.likeId(),
                snapshot.userId(),
                snapshot.targetId(),
                snapshot.active(),
                snapshot.updatedAt()
        );
    }

    /**
     * Intention métier : l’utilisateur veut être en état "liked" (true) ou "unliked" (false).
     * Si l’état ne change pas, on ne publie pas d’event.
     */
    public void set(boolean value, UUID commandId, Instant now) {
        if (this.active == value) {
            return; // rien à faire, donc pas d'événement
        }

        this.active = value;
        this.updatedAt = now;

        registerEvent(new LikeSetEvent(
                UUID.randomUUID(),  // ou un UUIDGenerator si tu veux
                commandId,
                this.id,            // likeId
                this.userId,
                this.targetId,
                this.active,
                this.updatedAt
        ));
    }

    public LikeSnapshot toSnapshot() {
        return new LikeSnapshot(
                this.id,
                this.userId,
                this.targetId,
                this.active,
                this.updatedAt
        );
    }

    public record LikeSnapshot(
            UUID likeId,
            UUID userId,
            UUID targetId,
            boolean active,
            Instant updatedAt
    ) {
    }
}
