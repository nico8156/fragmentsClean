package com.nm.fragmentsclean.socialContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.AggregateRoot;

import java.time.Instant;
import java.util.UUID;

public class Like extends AggregateRoot {

    private final UUID userId;
    private final UUID targetId;
    private boolean active;
    private Instant updatedAt;
    private long version; // version serveur de ce like (pour ce user/target)

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
        // nouveau like : pas encore actif, version 0
        return new Like(likeId, userId, targetId, false, now, 0L);
    }

    public static Like fromSnapshot(LikeSnapshot snapshot) {
        return new Like(
                snapshot.likeId(),
                snapshot.userId(),
                snapshot.targetId(),
                snapshot.active(),
                snapshot.updatedAt(),
                snapshot.version()
        );
    }

    /**
     * Intention métier : l’utilisateur veut être en état "liked" (true) ou "unliked" (false).
     * Si l’état ne change pas, on ne publie pas d’event.
     *
     * @param value     nouvel état demandé (true = LIKE, false = UNLIKE)
     * @param commandId id de la commande (idempotence / corrélation)
     * @param serverNow horodatage serveur (source de vérité temporelle)
     * @param clientAt  horodatage client de la commande
     * @param count     total des likes serveur pour ce target APRÈS cette modification
     */
    public void set(boolean value,
                    UUID commandId,
                    Instant serverNow,
                    Instant clientAt,
                    long count) {
        if (this.active == value) {
            return; // rien à faire, donc pas d'événement
        }

        this.active = value;
        this.updatedAt = serverNow;
        this.version++;

        registerEvent(new LikeSetEvent(
                UUID.randomUUID(), // eventId
                commandId,
                this.id,           // likeId (agrégat)
                this.userId,
                this.targetId,
                this.active,       // état serveur après traitement
                count,             // total des likes pour ce target
                this.version,      // version actuelle de ce like
                serverNow,         // occurredAt (serveur)
                clientAt           // clientAt (venant de la commande)
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
    ) {
    }
}
