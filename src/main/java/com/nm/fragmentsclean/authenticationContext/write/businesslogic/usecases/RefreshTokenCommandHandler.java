package com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.AuthUserRepository;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.JwtClaimsFactory;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.RefreshTokenRepository;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.TokenService;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.RefreshToken;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.CommandHandlerWithResult;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
	@Transactional
	public RefreshTokenResult execute(RefreshTokenCommand command) {
		if (command == null || command.refreshToken() == null || command.refreshToken().isBlank()) {
			throw new InvalidRefreshTokenException();
		}
		Instant now = dateTimeProvider.now();

		UUID familyId = refreshTokenRepository.findFamilyIdByToken(command.refreshToken())
				.orElseThrow(InvalidRefreshTokenException::new);
		refreshTokenRepository.lockFamily(familyId);

		RefreshToken existing = refreshTokenRepository.findByTokenForUpdate(command.refreshToken())
				.orElseThrow(InvalidRefreshTokenException::new);
		if (!existing.familyId().equals(familyId)) throw new InvalidRefreshTokenException();

		if (existing.revoked() || existing.isExpiredAt(now)) {
			throw new InvalidRefreshTokenException();
		}

		existing.revoke();
		refreshTokenRepository.save(existing);

		UUID userId = existing.userId();

		var authUser = authUserRepository.findById(userId)
				.orElseThrow(() -> new IllegalStateException("AuthUser not found for refresh token"));

		var claims = jwtClaimsFactory.forAuthUser(authUser);

		var tokenPair = tokenService.rotateTokensForUser(userId, claims, existing.familyId());

		return new RefreshTokenResult(
				tokenPair.accessToken(),
				tokenPair.refreshToken());
	}
}
