package com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.AuthUserRepository;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.JwtClaimsFactory;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.RefreshTokenRepository;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.TokenService;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.RefreshToken;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.CommandHandlerWithResult;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.AppUserRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class RefreshTokenCommandHandler
        implements CommandHandlerWithResult<RefreshTokenCommand, RefreshTokenResult> {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenService tokenService;
    private final DateTimeProvider dateTimeProvider;
    private final AppUserRepository appUserRepository;
    private final AuthUserRepository authUserRepository;
    private final JwtClaimsFactory jwtClaimsFactory;

    public RefreshTokenCommandHandler(
            RefreshTokenRepository refreshTokenRepository,
            TokenService tokenService,
            DateTimeProvider dateTimeProvider,
            AppUserRepository appUserRepository,
            AuthUserRepository authUserRepository,
            JwtClaimsFactory jwtClaimsFactory
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenService = tokenService;
        this.dateTimeProvider = dateTimeProvider;
        this.appUserRepository = appUserRepository;
        this.authUserRepository = authUserRepository;
        this.jwtClaimsFactory = jwtClaimsFactory;
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

        // 🔹 1. Charger l'AppUser par son ID (userId = AppUserId)
        var appUser = appUserRepository.findById(existing.userId())
                .orElseThrow(() -> new IllegalStateException("AppUser not found for refresh token"));

        // 🔹 2. Charger l'AuthUser via appUser.authUserId()
        var authUser = authUserRepository.findById(appUser.authUserId())
                .orElseThrow(() -> new IllegalStateException("AuthUser not found for refresh token"));

        // 🔹 3. Recréer les claims
        var claims = jwtClaimsFactory.forAuthUser(authUser);

        // 🔹 4. Générer un nouveau couple de tokens pour ce AppUser
        var tokenPair = tokenService.generateTokensForUser(appUser.id(), claims);

        return new RefreshTokenResult(
                tokenPair.accessToken(),
                tokenPair.refreshToken().token()
        );
    }
}

