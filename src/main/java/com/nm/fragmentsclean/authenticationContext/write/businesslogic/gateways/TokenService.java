package com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways;

import java.util.UUID;

public interface TokenService {
    TokenPair generateTokensForUser(UUID appUserId);

    record TokenPair(String accessToken, String refreshToken) {}
}
