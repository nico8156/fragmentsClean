package com.nm.fragmentsclean.socialContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.AggregateRoot;

import java.time.Instant;
import java.util.UUID;

public class Like extends AggregateRoot {

    private final UUID userId;
    private final UUID targetId;
    private boolean active;
    private Instant updatedAt;
    private long version;

    private Like(UUID likeId,
                 UUID userId,
                 UUID targetId,
                 boolean active,
                 Instant updatedAt,
                 long version) {
        super(likeId);
        this.userId = userId;
        this.targetId = targetId;
        this.active = active;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public static Like createNew(UUID likeId,
                                 UUID userId,
                                 UUID targetId,
                                 Instant now) {
        return new Like(likeId, userId, targetId, false, now, 0L);
    }

    public static Like fromSnapshot(LikeSnapshot snap) {
        return new Like(
                snap.likeId(),
                snap.userId(),
                snap.targetId(),
                snap.active(),
                snap.updatedAt(),
                snap.version()
        );
    }

    /**
     * Applique UNIQUEMENT l'état métier local
     * (sans count, sans event)
     */
    public boolean applyState(boolean value, Instant serverNow) {
        if (this.active == value) {
            return false; // aucun changement
        }

        this.active = value;
        this.updatedAt = serverNow;
        this.version++;

        return true;
    }

    /**
     * Enregistre l'événement après calcul du count
     */
    public void registerLikeSetEvent(UUID commandId,
                                     Instant clientAt,
                                     long count,
                                     Instant serverNow) {

        registerEvent(new LikeSetEvent(
                UUID.randomUUID(),   // eventId
                commandId,
                this.id,             // likeId
                this.userId,
                this.targetId,
                this.active,
                count,
                this.version,
                serverNow,
                clientAt
        ));
    }

    public LikeSnapshot toSnapshot() {
        return new LikeSnapshot(
                this.id,
                this.userId,
                this.targetId,
                this.active,
                this.updatedAt,
                this.version
        );
    }

    public record LikeSnapshot(
            UUID likeId,
            UUID userId,
            UUID targetId,
            boolean active,
            Instant updatedAt,
            long version
    ){}
}
