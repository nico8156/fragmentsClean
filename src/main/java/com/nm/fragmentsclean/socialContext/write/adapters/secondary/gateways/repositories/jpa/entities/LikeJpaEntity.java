package com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.UUID;

@Entity(name = "likes") // table "likes" dans ton schema.sql
@Getter
@NoArgsConstructor // obligatoire pour JPA
@ToString
@EqualsAndHashCode
public class LikeJpaEntity {

    @Id
    private UUID likeId;

    private UUID userId;
    private UUID targetId;
    private boolean active;
    private Instant updatedAt;
    private long version;

    public LikeJpaEntity(
            UUID likeId,
            UUID userId,
            UUID targetId,
            boolean active,
            Instant updatedAt,
            long version
    ) {
        this.likeId = likeId;
        this.userId = userId;
        this.targetId = targetId;
        this.active = active;
        this.updatedAt = updatedAt;
        this.version = version;
    }
}
