package com.nm.fragmentsclean.authContext.adapters.secondary.gateways.repositories.jpa.entities;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "identities")
public class IdentityJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "provider_user_id", nullable = false, length = 255)
    private String providerUserId;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_auth_at")
    private Instant lastAuthAt;

    protected IdentityJpaEntity() {
        // for JPA
    }

    public IdentityJpaEntity(UUID id,
                             UUID userId,
                             String provider,
                             String providerUserId,
                             String email,
                             Instant createdAt,
                             Instant lastAuthAt) {
        this.id = id;
        this.userId = userId;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
        this.createdAt = createdAt;
        this.lastAuthAt = lastAuthAt;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getProvider() { return provider; }
    public String getProviderUserId() { return providerUserId; }
    public String getEmail() { return email; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastAuthAt() { return lastAuthAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IdentityJpaEntity that)) return false;
        return Objects.equals(id, that.id)
                && Objects.equals(userId, that.userId)
                && Objects.equals(provider, that.provider)
                && Objects.equals(providerUserId, that.providerUserId)
                && Objects.equals(email, that.email)
                && Objects.equals(createdAt, that.createdAt)
                && Objects.equals(lastAuthAt, that.lastAuthAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, provider, providerUserId, email, createdAt, lastAuthAt);
    }
}
