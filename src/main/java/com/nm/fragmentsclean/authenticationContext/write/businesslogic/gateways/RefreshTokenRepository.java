package com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository {

    Optional<RefreshToken> findByToken(String token);

    RefreshToken save(RefreshToken refreshToken);
}
