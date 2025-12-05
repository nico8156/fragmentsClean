package com.nm.fragmentsclean.authenticationContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.AggregateRoot;

import java.time.Instant;
import java.util.UUID;

public class RefreshToken extends AggregateRoot {

    private final UUID userId;      // AppUser.id
    private final String token;     // valeur opaque renvoyée au client
    private final Instant expiresAt;
    private boolean revoked;

    private RefreshToken(UUID id,
                         UUID userId,
                         String token,
                         Instant expiresAt,
                         boolean revoked) {
        super(id);
        this.userId = userId;
        this.token = token;
        this.expiresAt = expiresAt;
        this.revoked = revoked;
    }

    public static RefreshToken createNew(UUID userId, String token, Instant expiresAt) {
        UUID id = UUID.randomUUID();
        return new RefreshToken(id, userId, token, expiresAt, false);
    }

    public static RefreshToken rehydrate(UUID id,
                                         UUID userId,
                                         String token,
                                         Instant expiresAt,
                                         boolean revoked) {
        return new RefreshToken(id, userId, token, expiresAt, revoked);
    }

    public UUID userId() {
        return userId;
    }

    public String token() {
        return token;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public boolean revoked() {
        return revoked;
    }

    public boolean isExpiredAt(Instant now) {
        return now.isAfter(expiresAt);
    }

    public void revoke() {
        this.revoked = true;
    }
}
