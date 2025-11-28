package com.nm.fragmentsclean.authContext.write.businesslogic.models;

import java.time.Instant;
import java.util.UUID;

public class Identity {

    private final UUID id;
    private UUID userId;              // lien vers AppUser
    private final String provider;    // "google"
    private final String providerUserId; // sub Google

    private String email;
    private final Instant createdAt;
    private Instant lastAuthAt;

    public Identity(
            UUID id,
            UUID userId,
            String provider,
            String providerUserId,
            String email,
            Instant createdAt,
            Instant lastAuthAt
    ) {
        this.id = id;
        this.userId = userId;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
        this.createdAt = createdAt;
        this.lastAuthAt = lastAuthAt;
    }

    public static Identity createNew(VerifiedOAuthProfile profile, UUID userId, Instant now) {
        return new Identity(
                UUID.randomUUID(),
                userId,
                profile.provider(),
                profile.providerUserId(),
                profile.email(),
                now,
                now
        );
    }

    public void markAuthenticatedAt(Instant now) {
        this.lastAuthAt = now;
    }

    // getters simples (ou records + snapshots plus tard si tu veux)
    public UUID id() { return id; }
    public UUID userId() { return userId; }
    public String provider() { return provider; }
    public String providerUserId() { return providerUserId; }
    public String email() { return email; }
    public Instant createdAt() { return createdAt; }
    public Instant lastAuthAt() { return lastAuthAt; }
}
