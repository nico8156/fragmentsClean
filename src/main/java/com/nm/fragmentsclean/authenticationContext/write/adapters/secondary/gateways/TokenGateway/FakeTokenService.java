package com.nm.fragmentsclean.authenticationContext.write.adapters.secondary.gateways.TokenGateway;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.RefreshTokenRepository;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.RefreshTokenHasher;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.TokenService;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.JwtClaims;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.RefreshToken;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile("fake-token") // 🔹 plus "test", un profil spécial que tu actives seulement si tu veux le fake
public class FakeTokenService implements TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final DateTimeProvider clock;
    private final RefreshTokenHasher refreshTokenHasher;

    public FakeTokenService(RefreshTokenRepository refreshTokenRepository,
                            DateTimeProvider clock,
                            RefreshTokenHasher refreshTokenHasher) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.clock = clock;
        this.refreshTokenHasher = refreshTokenHasher;
    }

    @Override
    public TokenPair generateTokensForUser(UUID appUserId, JwtClaims claims) {
        return issueTokens(appUserId, UUID.randomUUID());
    }

    @Override
    public TokenPair rotateTokensForUser(
            UUID appUserId, JwtClaims claims, UUID refreshTokenFamilyId) {
        return issueTokens(appUserId, refreshTokenFamilyId);
    }

    private TokenPair issueTokens(UUID appUserId, UUID refreshTokenFamilyId) {
        String access = "access-" + appUserId + "-" + System.currentTimeMillis();
        String refreshValue = "refresh-" + appUserId + "-" + System.currentTimeMillis();

        var now = clock.now();
        var expiresAt = now.plusSeconds(7 * 24 * 3600); // 7 jours pour l’exemple

        var refreshToken = RefreshToken.createInFamily(
                appUserId, refreshTokenHasher.hash(refreshValue), expiresAt, refreshTokenFamilyId);
        refreshTokenRepository.save(refreshToken);

        return new TokenPair(access, refreshValue);
    }
}
