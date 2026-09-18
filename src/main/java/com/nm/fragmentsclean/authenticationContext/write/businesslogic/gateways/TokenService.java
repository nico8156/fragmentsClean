package com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.JwtClaims;
import java.util.UUID;

public interface TokenService {
    TokenPair generateTokensForUser(UUID appUserId, JwtClaims claims);

    TokenPair rotateTokensForUser(
            UUID appUserId, JwtClaims claims, UUID refreshTokenFamilyId);

    record TokenPair(String accessToken, String refreshToken) {}

}
