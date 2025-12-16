package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.AggregateRoot;

import java.time.Instant;
import java.util.UUID;

public class AppUser extends AggregateRoot {

    private final UUID authUserId;
    private String displayName;
    private String avatarUrl;      // NEW
    private final Instant createdAt;
    private Instant updatedAt;     // NEW
    private long version;          // NEW

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
        var user = new AppUser(id, authUserId, displayName, avatarUrl, now, now, 0L);

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

    /** Update public profile: idempotent + event si changement */
    public boolean updatePublicProfile(String newDisplayName, String newAvatarUrl, Instant now) {
        boolean changed = false;

        if (newDisplayName != null && !newDisplayName.isBlank() && !newDisplayName.equals(this.displayName)) {
            this.displayName = newDisplayName;
            changed = true;
        }

        if (newAvatarUrl != null && !newAvatarUrl.equals(this.avatarUrl)) {
            this.avatarUrl = newAvatarUrl;
            changed = true;
        }

        if (changed) {
            this.updatedAt = now;
            this.version++;

            this.registerEvent(new AppUserProfileUpdatedEvent(
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
}
