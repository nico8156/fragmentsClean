package com.nm.fragmentsclean.authenticationContext.write.businesslogic.models;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;

public record JwtClaims(
        String subject,          // authUserId
        String email,
        Set<AuthRole> roles,
        Set<String> scopes,
        Instant issuedAt,
        Instant expiresAt
) {
    public JwtClaims {
        roles = roles == null ? Set.of() : Collections.unmodifiableSet(roles);
        scopes = scopes == null ? Set.of() : Collections.unmodifiableSet(scopes);
    }

    public boolean hasRole(AuthRole role) {
        return roles.contains(role);
    }

    public boolean hasScope(String scope) {
        return scopes.contains(scope);
    }
}
