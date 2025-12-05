package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.AggregateRoot;

import java.time.Instant;
import java.util.UUID;

public class AppUser extends AggregateRoot {

    private final UUID authUserId; // lien vers AuthUser
    private String displayName;
    private final Instant createdAt;

    public AppUser(UUID id, UUID authUserId, String displayName, Instant createdAt) {
        super(id);
        this.authUserId = authUserId;
        this.displayName = displayName;
        this.createdAt = createdAt;
    }

    public static AppUser createNew(UUID authUserId, String displayName, Instant now) {
        UUID id = UUID.randomUUID();
        var user = new AppUser(id, authUserId, displayName, now);

        user.registerEvent(AppUserCreatedEvent.of(user, now));

        return user;
    }

    public UUID authUserId() {
        return authUserId;
    }

    public String displayName() {
        return displayName;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public void rename(String newName) {
        this.displayName = newName;
        // éventuellement : registerEvent(new AppUserRenamedEvent(...));
    }
}
