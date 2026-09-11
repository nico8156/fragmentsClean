package com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.RefreshToken;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

  Optional<RefreshToken> findByToken(String token);

  RefreshToken save(RefreshToken refreshToken);

  List<RefreshToken> findAllByUserId(UUID userId);
}
