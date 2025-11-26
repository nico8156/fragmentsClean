package com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.entities;

import com.nm.fragmentsclean.socialContext.write.businesslogic.models.ModerationStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Entity(name = "comments") // table "comments" dans ton schema.sql
@Getter
@NoArgsConstructor // obligatoire pour JPA
@ToString
@EqualsAndHashCode
public class CommentJpaEntity {

    @Id
    private UUID commentId;

    private UUID targetId;
    private UUID authorId;
    private UUID parentId;   // nullable OK

    private String body;

    private Instant createdAt;
    private Instant editedAt;
    private Instant deletedAt;

    @Enumerated(EnumType.STRING)
    private ModerationStatus moderation;

    private long version;

    public CommentJpaEntity(
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
    ) {
        this.commentId = commentId;
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
}
