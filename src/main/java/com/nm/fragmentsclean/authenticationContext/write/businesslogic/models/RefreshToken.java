package com.nm.fragmentsclean.authenticationContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.AggregateRoot;

import java.time.Instant;
import java.util.UUID;

public class RefreshToken extends AggregateRoot {

    private final UUID userId;      // AppUser.id
    private final UUID familyId;
    private final String tokenHash;
    private final Instant expiresAt;
    private boolean revoked;

    private RefreshToken(UUID id,
                         UUID userId,
                         UUID familyId,
                         String tokenHash,
                         Instant expiresAt,
                         boolean revoked) {
        super(id);
        if (userId == null) throw new IllegalArgumentException("Refresh token user is required");
        if (familyId == null) throw new IllegalArgumentException("Refresh token family is required");
        if (tokenHash == null || !tokenHash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("Refresh token hash must be a lowercase SHA-256 value");
        }
        if (expiresAt == null) throw new IllegalArgumentException("Refresh token expiry is required");
        this.userId = userId;
        this.familyId = familyId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.revoked = revoked;
    }

    public static RefreshToken createNew(UUID userId, String tokenHash, Instant expiresAt) {
        return createInFamily(userId, tokenHash, expiresAt, UUID.randomUUID());
    }

    public static RefreshToken createInFamily(
            UUID userId, String tokenHash, Instant expiresAt, UUID familyId) {
        UUID id = UUID.randomUUID();
        return new RefreshToken(id, userId, familyId, tokenHash, expiresAt, false);
    }

    public static RefreshToken rehydrate(UUID id,
                                         UUID userId,
                                         UUID familyId,
                                         String tokenHash,
                                         Instant expiresAt,
                                         boolean revoked) {
        return new RefreshToken(id, userId, familyId, tokenHash, expiresAt, revoked);
    }

    public UUID userId() {
        return userId;
    }

    public UUID familyId() {
        return familyId;
    }

    public String tokenHash() {
        return tokenHash;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public boolean revoked() {
        return revoked;
    }

    public boolean isExpiredAt(Instant now) {
        return !now.isBefore(expiresAt);
    }

    public void revoke() {
        this.revoked = true;
    }
}
