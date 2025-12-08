package com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.TokenGateway;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.RefreshTokenRepository;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.TokenService;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.RefreshToken;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile("test") // ou "dev"
public class FakeTokenService implements TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final DateTimeProvider clock;

    public FakeTokenService(RefreshTokenRepository refreshTokenRepository, DateTimeProvider clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.clock = clock;
    }

    @Override
    public TokenPair generateTokensForUser(UUID appUserId) {
        String access = "access-" + appUserId + "-" + System.currentTimeMillis();
        String refreshValue = "refresh-" + appUserId + "-" + System.currentTimeMillis();

        var now = clock.now();
        var expiresAt = now.plusSeconds(7 * 24 * 3600); // 7 jours pour l’exemple

        var refreshToken = RefreshToken.createNew(appUserId, refreshValue, expiresAt);
        refreshTokenRepository.save(refreshToken);

        return new TokenPair(access, refreshToken);
    }
}
