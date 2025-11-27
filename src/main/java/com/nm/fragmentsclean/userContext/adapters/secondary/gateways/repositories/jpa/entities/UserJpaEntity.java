package com.nm.fragmentsclean.userContext.adapters.secondary.gateways.repositories.jpa.entities;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "bio")
    private String bio;

    @Column(name = "locale", nullable = false)
    private String locale;

    @Column(name = "version", nullable = false)
    private long version;

    protected UserJpaEntity() {
        // for JPA
    }

    public UserJpaEntity(
            UUID id,
            Instant createdAt,
            Instant updatedAt,
            String displayName,
            String avatarUrl,
            String bio,
            String locale,
            long version
    ) {
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.bio = bio;
        this.locale = locale;
        this.version = version;
    }

    public UUID getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getBio() {
        return bio;
    }

    public String getLocale() {
        return locale;
    }

    public long getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserJpaEntity that)) return false;
        return version == that.version
                && Objects.equals(id, that.id)
                && Objects.equals(createdAt, that.createdAt)
                && Objects.equals(updatedAt, that.updatedAt)
                && Objects.equals(displayName, that.displayName)
                && Objects.equals(avatarUrl, that.avatarUrl)
                && Objects.equals(bio, that.bio)
                && Objects.equals(locale, that.locale);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, createdAt, updatedAt, displayName, avatarUrl, bio, locale, version);
    }
}
