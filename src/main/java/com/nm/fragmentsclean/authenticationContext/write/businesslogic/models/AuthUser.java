package com.nm.fragmentsclean.authenticationContext.write.businesslogic.models;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.AggregateRoot;

import java.time.Instant;
import java.util.UUID;

public class AuthUser extends AggregateRoot {

    private final AuthProvider provider;
    private final String providerUserId; // Google sub
    private final String email;
    private final boolean emailVerified;
    private Instant lastLoginAt;

    private AuthUser(UUID id,
                     AuthProvider provider,
                     String providerUserId,
                     String email,
                     boolean emailVerified,
                     Instant lastLoginAt) {
        super(id);
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
        this.emailVerified = emailVerified;
        this.lastLoginAt = lastLoginAt;
    }

    public static AuthUser createNew(AuthProvider provider,
                                     String providerUserId,
                                     String email,
                                     boolean emailVerified,
                                     Instant now) {
        UUID id = UUID.randomUUID();
        var authUser = new AuthUser(id, provider, providerUserId, email, emailVerified, now);

        // Ici aussi tu peux émettre un event domaine si tu veux :
        // authUser.registerEvent(new AuthUserCreatedEvent(...));

        return authUser;
    }

    public void markLogin(Instant now) {
        this.lastLoginAt = now;
        // registerEvent(new AuthUserLoggedInEvent(this.id(), now));
    }

    public AuthProvider provider() {
        return provider;
    }

    public String providerUserId() {
        return providerUserId;
    }

    public String email() {
        return email;
    }

    public boolean emailVerified() {
        return emailVerified;
    }

    public Instant lastLoginAt() {
        return lastLoginAt;
    }
}
