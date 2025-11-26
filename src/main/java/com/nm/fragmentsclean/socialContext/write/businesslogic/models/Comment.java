package com.nm.fragmentsclean.socialContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.AggregateRoot;

import java.time.Instant;
import java.util.UUID;

public class Comment extends AggregateRoot {
    private final UUID targetId;
    private final UUID authorId;
    private final UUID parentId;       // peut être null

    private String body;
    private final Instant createdAt;
    private Instant editedAt;
    private Instant deletedAt;
    private ModerationStatus moderation;
    private long version;

    private Comment(UUID commentId,
                    UUID targetId,
                    UUID authorId,
                    UUID parentId,
                    String body,
                    Instant createdAt,
                    Instant editedAt,
                    Instant deletedAt,
                    ModerationStatus moderation,
                    long version) {
        super(commentId);
        this.targetId = targetId;
        this.authorId = authorId;
        this.parentId = parentId;
        this.body = body;
        this.createdAt = createdAt;
        this.editedAt = editedAt;
        this.deletedAt = deletedAt;
        this.moderation = moderation;
        this.version = version;
    }

    public static Comment createNew(UUID commentId,
                                    UUID targetId,
                                    UUID authorId,
                                    UUID parentId,
                                    String body,
                                    Instant now) {
        return new Comment(
                commentId,
                targetId,
                authorId,
                parentId,
                body,
                now,
                null,
                null,
                ModerationStatus.PUBLISHED, // comme le front pour le moment
                0L
        );
    }

    public static Comment fromSnapshot(CommentSnapshot snap) {
        return new Comment(
                snap.commentId(),
                snap.targetId(),
                snap.authorId(),
                snap.parentId(),
                snap.body(),
                snap.createdAt(),
                snap.editedAt(),
                snap.deletedAt(),
                snap.moderation(),
                snap.version()
        );
    }

    /**
     * Update : applique uniquement l’état métier local (body, editedAt, version)
     * @return true si changement, false si idempotent
     */
    public boolean applyBodyEdit(String newBody, Instant now) {
        if (newBody == null || newBody.isBlank()) {
            throw new IllegalArgumentException("Comment body cannot be empty");
        }
        if (newBody.equals(this.body)) {
            return false; // aucun changement
        }

        this.body = newBody;
        this.editedAt = now;
        this.version++;

        return true;
    }
    /**
     * Delete "soft" : marque comme supprimé côté domaine
     * @return true si changement, false si déjà supprimé
     */
    public boolean softDelete(Instant now) {
        if (this.deletedAt != null) {
            return false; // déjà soft-deleted
        }
        this.deletedAt = now;
        this.moderation = ModerationStatus.SOFT_DELETED;
        this.version++;

        return true;
    }


    public void registerCreatedEvent(UUID commandId,
                                     Instant clientAt,
                                     Instant serverNow) {
        registerEvent(new CommentCreatedEvent(
                UUID.randomUUID(),    // eventId
                commandId,
                this.id,              // commentId
                this.targetId,
                this.parentId,
                this.authorId,
                this.body,
                this.moderation,
                this.version,
                serverNow,
                clientAt
        ));
    }
    public void registerUpdatedEvent(UUID commandId,
                                     Instant clientAt,
                                     Instant serverNow) {

        registerEvent(new CommentUpdatedEvent(
                UUID.randomUUID(),
                commandId,
                this.id,
                this.targetId,
                this.authorId,
                this.body,
                this.moderation,
                this.version,
                serverNow,
                clientAt
        ));
    }

    public void registerDeletedEvent(UUID commandId,
                                     Instant clientAt,
                                     Instant serverNow) {

        registerEvent(new CommentDeletedEvent(
                UUID.randomUUID(),
                commandId,
                this.id,
                this.targetId,
                this.authorId,
                this.moderation,
                this.deletedAt,
                this.version,
                serverNow,
                clientAt
        ));
    }


    public CommentSnapshot toSnapshot() {
        return new CommentSnapshot(
                this.id,
                this.targetId,
                this.authorId,
                this.parentId,
                this.body,
                this.createdAt,
                this.editedAt,
                this.deletedAt,
                this.moderation,
                this.version
        );
    }

    public record CommentSnapshot(
            UUID commentId,
            UUID targetId,
            UUID authorId,
            UUID parentId,
            String body,
            Instant createdAt,
            Instant editedAt,
            Instant deletedAt,
            ModerationStatus moderation,
            long version
    ) {}
}
