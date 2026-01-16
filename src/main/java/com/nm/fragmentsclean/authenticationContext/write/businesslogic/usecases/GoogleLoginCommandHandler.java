package com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases;

import org.springframework.stereotype.Component;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.AuthUserRepository;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.GoogleAuthService;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.JwtClaimsFactory;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.TokenService;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthProvider;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthUser;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.CommandHandlerWithResult;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;

@Component
public class GoogleLoginCommandHandler implements CommandHandlerWithResult<GoogleLoginCommand, GoogleLoginResult> {

	private final DomainEventPublisher domainEventPublisher;
	private final GoogleAuthService googleAuthService;
	private final AuthUserRepository authUserRepository;
	private final TokenService tokenService;
	private final DateTimeProvider dateTimeProvider;
	private final JwtClaimsFactory jwtClaimsFactory;

	public GoogleLoginCommandHandler(
			DomainEventPublisher domainEventPublisher,
			GoogleAuthService googleAuthService,
			AuthUserRepository authUserRepository,
			TokenService tokenService,
			DateTimeProvider dateTimeProvider,
			JwtClaimsFactory jwtClaimsFactory) {
		this.domainEventPublisher = domainEventPublisher;
		this.googleAuthService = googleAuthService;
		this.authUserRepository = authUserRepository;
		this.tokenService = tokenService;
		this.dateTimeProvider = dateTimeProvider;
		this.jwtClaimsFactory = jwtClaimsFactory;
	}

	@Override
	public GoogleLoginResult execute(GoogleLoginCommand command) {
		var now = dateTimeProvider.now();

		// 1) Exchange authorizationCode -> Google user info
		var google = googleAuthService.exchangeCodeForUser(command.authorizationCode());

		// 2) Upsert AuthUser (auth/security aggregate)
		AuthUser authUser = authUserRepository
				.findByProviderAndProviderUserId(AuthProvider.GOOGLE, google.sub())
				.map(existing -> {
					existing.markLogin(now);
					authUserRepository.save(existing);
					return existing;
				})
				.orElseGet(() -> {
					var created = AuthUser.createNew(
							AuthProvider.GOOGLE,
							google.sub(),
							google.email(),
							google.emailVerified(),
							now);
					authUserRepository.save(created);
					return created;
				});

		// 3) Publish only AuthUser events (outbox -> kafka)
		authUser.domainEvents().forEach(domainEventPublisher::publish);
		authUser.clearDomainEvents();

		// 4) Claims from AuthUser
		var claims = jwtClaimsFactory.forAuthUser(authUser);

		// 5) Tokens subject = authUser.id (=> future: AppUser.id == AuthUser.id)
		var tokens = tokenService.generateTokensForUser(authUser.id(), claims);

		// 6) Result for HTTP adapter (no AppUser repo here)
		return new GoogleLoginResult(
				tokens.accessToken(),
				tokens.refreshToken().token(),
				authUser.id(),
				google.name(),
				google.email(),
				google.pictureUrl());
	}
}
