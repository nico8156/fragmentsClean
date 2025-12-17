package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.AggregateRoot;

import java.time.Instant;
import java.util.UUID;

public class AppUser extends AggregateRoot {

    private final UUID authUserId;
    private String displayName;
    private String avatarUrl;
    private final Instant createdAt;
    private Instant updatedAt;
    private long version;

    public AppUser(UUID id,
                   UUID authUserId,
                   String displayName,
                   String avatarUrl,
                   Instant createdAt,
                   Instant updatedAt,
                   long version) {
        super(id);
        this.authUserId = authUserId;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public static AppUser createNew(UUID authUserId, String displayName, String avatarUrl, Instant now) {
        UUID id = UUID.randomUUID();
        var user = new AppUser(
                id,
                authUserId,
                normalizeDisplayName(displayName),
                avatarUrl,
                now,
                now,
                0L
        );

        user.registerEvent(new AppUserCreatedEvent(
                UUID.randomUUID(),
                user.id(),
                user.authUserId(),
                user.displayName(),
                user.avatarUrl(),
                user.version(),
                now
        ));

        return user;
    }

    public UUID authUserId() { return authUserId; }
    public String displayName() { return displayName; }
    public String avatarUrl() { return avatarUrl; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public long version() { return version; }

    /** Idempotent update + event if changed */
    public boolean updatePublicProfile(String newDisplayName, String newAvatarUrl, Instant now) {
        boolean changed = false;

        String normalized = normalizeDisplayName(newDisplayName);
        if (normalized != null && !normalized.equals(this.displayName)) {
            this.displayName = normalized;
            changed = true;
        }

        if (newAvatarUrl != null && !newAvatarUrl.equals(this.avatarUrl)) {
            this.avatarUrl = newAvatarUrl;
            changed = true;
        }

        if (changed) {
            this.updatedAt = now;
            this.version++;

            registerEvent(new AppUserProfileUpdatedEvent(
                    UUID.randomUUID(),
                    this.id,
                    this.displayName,
                    this.avatarUrl,
                    this.version,
                    now
            ));
        }

        return changed;
    }

    private static String normalizeDisplayName(String name) {
        if (name == null) return null;
        String trimmed = name.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
