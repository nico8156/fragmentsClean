package com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.RefreshTokenRepository;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.TokenService;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.RefreshToken;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.CommandHandlerWithResult;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class RefreshTokenCommandHandler
        implements CommandHandlerWithResult<RefreshTokenCommand, RefreshTokenResult> {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenService tokenService;
    private final DateTimeProvider dateTimeProvider;

    public RefreshTokenCommandHandler(RefreshTokenRepository refreshTokenRepository,
                                      TokenService tokenService,
                                      DateTimeProvider dateTimeProvider) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenService = tokenService;
        this.dateTimeProvider = dateTimeProvider;
    }

    @Override
    public RefreshTokenResult execute(RefreshTokenCommand command) {
        Instant now = dateTimeProvider.now();

        RefreshToken existing = refreshTokenRepository.findByToken(command.refreshToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (existing.revoked() || existing.isExpiredAt(now)) {
            throw new IllegalArgumentException("Refresh token expired or revoked");
        }

        // Rotation : on révoque l’ancien
        existing.revoke();
        refreshTokenRepository.save(existing);

        // Génère un nouveau couple pour ce user
        var tokenPair = tokenService.generateTokensForUser(existing.userId());

        return new RefreshTokenResult(
                tokenPair.accessToken(),
                tokenPair.refreshToken().token()
        );
    }
}
