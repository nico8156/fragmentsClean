package com.nm.fragmentsclean.authContext.adapters.secondary.gateways.fake;

import com.nm.fragmentsclean.authContext.businesslogic.gateways.JwtTokenGenerator;
import com.nm.fragmentsclean.authContext.businesslogic.models.AppSessionTokens;
import com.nm.fragmentsclean.authContext.businesslogic.models.Identity;
import com.nm.fragmentsclean.userContext.businesslogic.models.AppUser;

import java.time.Instant;

public class FakeJwtTokenGenerator implements JwtTokenGenerator {

    @Override
    public AppSessionTokens generateAccessToken(AppUser user, Identity identity, Instant now) {
        long issuedAt = now.getEpochSecond();
        long expiresAt = issuedAt + 3600; // 1h

        // On évite de dépendre de la structure interne des modèles :
        String userPart = (user != null) ? Integer.toHexString(user.hashCode()) : "nouser";
        String identityPart = (identity != null) ? Integer.toHexString(identity.hashCode()) : "noidentity";

        String base = issuedAt + ":" + userPart + ":" + identityPart;

        String accessToken  = "fake-access-"   + base;
        String idToken      = "fake-id-"       + base;
        String refreshToken = "fake-refresh-"  + base;

        String tokenType = "Bearer";
        String scope = "openid profile email";

        return new AppSessionTokens(
                accessToken,
                idToken,
                refreshToken,
                expiresAt,
                issuedAt,
                tokenType,
                scope
        );
    }
}
