package com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.AuthUserRepository;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.GoogleAuthService;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.JwtClaimsFactory;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.TokenService;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthProvider;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthUser;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.CommandHandlerWithResult;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;

import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.AppUserRepository;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUser;
import org.springframework.stereotype.Component;


@Component
public class GoogleLoginCommandHandler implements CommandHandlerWithResult<GoogleLoginCommand, GoogleLoginResult> {

    private final DomainEventPublisher domainEventPublisher;
    private final GoogleAuthService googleAuthService;
    private final AuthUserRepository authUserRepository;
    private final AppUserRepository appUserRepository;
    private final TokenService tokenService;
    private final DateTimeProvider dateTimeProvider;
    private final JwtClaimsFactory jwtClaimsFactory;   // 👈 nouveau


    public GoogleLoginCommandHandler(
            DomainEventPublisher domainEventPublisher,
            GoogleAuthService googleAuthService,
            AuthUserRepository authUserRepository,
            AppUserRepository appUserRepository,
            TokenService tokenService,
            DateTimeProvider dateTimeProvider,
            JwtClaimsFactory jwtClaimsFactory) {
        this.domainEventPublisher = domainEventPublisher;
        this.googleAuthService = googleAuthService;
        this.authUserRepository = authUserRepository;
        this.appUserRepository = appUserRepository;
        this.tokenService = tokenService;
        this.dateTimeProvider = dateTimeProvider;
        this.jwtClaimsFactory = jwtClaimsFactory;
    }

    @Override
    public GoogleLoginResult execute(GoogleLoginCommand command) {
        var now = dateTimeProvider.now();

        // 1. Échange code → user Google
        var google = googleAuthService.exchangeCodeForUser(
                command.code(),
                command.codeVerifier(),
                command.redirectUri()
        );

        // 2. AuthUser (porteur des events)
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
                            now
                    );
                    authUserRepository.save(created);
                    return created;
                });

        // 3. AppUser
        AppUser appUser = appUserRepository
                .findByAuthUserId(authUser.id())
                .orElseGet(() -> {
                    var created = AppUser.createNew(authUser.id(), google.name(), now);
                    appUserRepository.save(created);
                    return created;
                });

        // 3 BIS : publier les events
        authUser.domainEvents().forEach(domainEventPublisher::publish);
        authUser.clearDomainEvents();

        appUser.domainEvents().forEach(domainEventPublisher::publish);
        appUser.clearDomainEvents();

        // 🔹 4. Construire les claims (roles/scopes) au niveau domaine
        var claims = jwtClaimsFactory.forAuthUser(authUser);

        // 🔹 5. Générer les tokens à partir de l'appUserId + claims
        var tokens = tokenService.generateTokensForUser(appUser.id(), claims);

        // 6. Résultat
        return new GoogleLoginResult(
                tokens.accessToken(),
                tokens.refreshToken().token(),
                appUser.id(),
                appUser.displayName(),
                google.email(),
                google.pictureUrl()
        );
    }
}
