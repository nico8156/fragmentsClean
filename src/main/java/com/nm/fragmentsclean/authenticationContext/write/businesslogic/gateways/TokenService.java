package com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.JwtClaims;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.RefreshToken;

import java.util.UUID;

public interface TokenService {
    TokenPair generateTokensForUser(UUID appUserId, JwtClaims claims);

    record TokenPair(String accessToken, RefreshToken refreshToken) {}

}
