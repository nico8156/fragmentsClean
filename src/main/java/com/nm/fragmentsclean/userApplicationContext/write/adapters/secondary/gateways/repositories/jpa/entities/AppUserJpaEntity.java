package com.nm.fragmentsclean.userApplicationContext.write.adapters.secondary.gateways.repositories.jpa.entities;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_users")
public class AppUserJpaEntity {

    @Id
    private UUID id;

    @Column(name = "auth_user_id", nullable = false)
    private UUID authUserId;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AppUserJpaEntity() {
    }

    public AppUserJpaEntity(UUID id, UUID authUserId, String displayName, Instant createdAt) {
        this.id = id;
        this.authUserId = authUserId;
        this.displayName = displayName;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAuthUserId() {
        return authUserId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
