package com.nm.fragmentsclean.socialContext.write.adapters.secondary.gateways.repositories.jpa.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "user_blocks")
@Getter @NoArgsConstructor
public class UserBlockJpaEntity {
    @Id private UUID blockId;
    private UUID blockerId;
    private UUID blockedUserId;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
    private long version;

    public UserBlockJpaEntity(UUID blockId, UUID blockerId, UUID blockedUserId, boolean active,
                              Instant createdAt, Instant updatedAt, long version) {
        this.blockId = blockId; this.blockerId = blockerId; this.blockedUserId = blockedUserId;
        this.active = active; this.createdAt = createdAt; this.updatedAt = updatedAt; this.version = version;
    }
}
