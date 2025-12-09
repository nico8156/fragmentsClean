package com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.TokenGateway;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.JwtClaimsFactory;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthRole;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthUser;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.JwtClaims;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

public class DefaultJwtClaimsFactory implements JwtClaimsFactory {

    private final DateTimeProvider dateTimeProvider;
    private final Duration accessTokenTtl;

    public DefaultJwtClaimsFactory(DateTimeProvider dateTimeProvider,
                                   Duration accessTokenTtl) {
        this.dateTimeProvider = dateTimeProvider;
        this.accessTokenTtl = accessTokenTtl;
    }

    @Override
    public JwtClaims forAuthUser(AuthUser authUser) {
        Instant now = dateTimeProvider.now();
        Instant expiresAt = now.plus(accessTokenTtl);

        // 🔹 Rôle par défaut : USER
        Set<AuthRole> roles = Set.of(AuthRole.USER);

        // 🔹 Scopes gamification (tu ajusteras)
        Set<String> scopes = Set.of(
                "gamification:read",
                "gamification:earn"  // par ex: gagner des points / badges
        );

        return new JwtClaims(
                authUser.id().toString(),      // subject
                authUser.email(),      // à adapter selon ton modèle
                roles,
                scopes,
                now,
                expiresAt
        );
    }
}
