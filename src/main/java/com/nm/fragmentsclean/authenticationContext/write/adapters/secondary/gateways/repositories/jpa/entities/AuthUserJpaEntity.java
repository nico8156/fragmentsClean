package com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.repositories.jpa.entities;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthProvider;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "auth_users")
public class AuthUserJpaEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;

    @Column(nullable = false)
    private String email;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "last_login_at", nullable = false)
    private Instant lastLoginAt;

    protected AuthUserJpaEntity() {
        // for JPA
    }

    public AuthUserJpaEntity(UUID id,
                             AuthProvider provider,
                             String providerUserId,
                             String email,
                             boolean emailVerified,
                             Instant lastLoginAt) {
        this.id = id;
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
        this.emailVerified = emailVerified;
        this.lastLoginAt = lastLoginAt;
    }

    public UUID getId() {
        return id;
    }

    public AuthProvider getProvider() {
        return provider;
    }

    public String getProviderUserId() {
        return providerUserId;
    }

    public String getEmail() {
        return email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }
}
