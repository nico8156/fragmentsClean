package com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.AuthUserRepository;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.JwtClaimsFactory;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.RefreshTokenRepository;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.TokenService;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.RefreshToken;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.CommandHandlerWithResult;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class RefreshTokenCommandHandler
		implements CommandHandlerWithResult<RefreshTokenCommand, RefreshTokenResult> {

	private final RefreshTokenRepository refreshTokenRepository;
	private final TokenService tokenService;
	private final DateTimeProvider dateTimeProvider;
	private final AuthUserRepository authUserRepository;
	private final JwtClaimsFactory jwtClaimsFactory;

	public RefreshTokenCommandHandler(
			RefreshTokenRepository refreshTokenRepository,
			TokenService tokenService,
			DateTimeProvider dateTimeProvider,
			AuthUserRepository authUserRepository,
			JwtClaimsFactory jwtClaimsFactory) {
		this.refreshTokenRepository = refreshTokenRepository;
		this.tokenService = tokenService;
		this.dateTimeProvider = dateTimeProvider;
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

		// rotation : révoque l’ancien
		existing.revoke();
		refreshTokenRepository.save(existing);

		// ✅ userId stocké dans refresh_token = subject = authUserId (= appUserId)
		UUID userId = existing.userId();

		var authUser = authUserRepository.findById(userId)
				.orElseThrow(() -> new IllegalStateException("AuthUser not found for refresh token"));

		var claims = jwtClaimsFactory.forAuthUser(authUser);

		var tokenPair = tokenService.generateTokensForUser(userId, claims);

		return new RefreshTokenResult(
				tokenPair.accessToken(),
				tokenPair.refreshToken().token());
	}
}
