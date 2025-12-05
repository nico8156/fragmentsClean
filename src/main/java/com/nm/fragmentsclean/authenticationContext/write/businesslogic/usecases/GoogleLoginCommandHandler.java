package com.nm.fragmentsclean.authenticationContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.AuthUserRepository;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.GoogleAuthService;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.gateways.TokenService;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthProvider;
import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthUser;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.AppUserRepository;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUser;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
public class GoogleLoginCommandHandler {

    private final GoogleAuthService googleAuthService;
    private final AuthUserRepository authUserRepository;
    private final AppUserRepository appUserRepository;
    private final TokenService tokenService;
    private final Clock clock;

    public GoogleLoginCommandHandler(GoogleAuthService googleAuthService,
                                     AuthUserRepository authUserRepository,
                                     AppUserRepository appUserRepository,
                                     TokenService tokenService,
                                     Clock clock) {
        this.googleAuthService = googleAuthService;
        this.authUserRepository = authUserRepository;
        this.appUserRepository = appUserRepository;
        this.tokenService = tokenService;
        this.clock = clock;
    }

    public GoogleLoginResult handle(GoogleLoginCommand command) {
        Instant now = Instant.now(clock);

        // 1. Échanger le code contre un user Google
        var google = googleAuthService.exchangeCodeForUser(
                command.code(),
                command.codeVerifier(),
                command.redirectUri()
        );

        // 2. Résoudre / créer AuthUser
        AuthUser authUser = authUserRepository
                .findByProviderAndProviderUserId(AuthProvider.GOOGLE, google.sub())
                .map(existing -> {
                    existing.markLogin(now);
                    return authUserRepository.save(existing);
                })
                .orElseGet(() -> {
                    var created = AuthUser.createNew(
                            AuthProvider.GOOGLE,
                            google.sub(),
                            google.email(),
                            google.emailVerified(),
                            now
                    );
                    return authUserRepository.save(created);
                });

        // 3. Résoudre / créer AppUser
        AppUser appUser = appUserRepository
                .findByAuthUserId(authUser.id())
                .orElseGet(() -> {
                    var created = AppUser.createNew(authUser.id(), google.name(), now);
                    return appUserRepository.save(created);
                });

        // 4. Générer les tokens
        var tokens = tokenService.generateTokensForUser(appUser.id());

        // 5. Retourner le résultat
        return new GoogleLoginResult(
                tokens.accessToken(),
                tokens.refreshToken(),
                appUser.id(),
                appUser.displayName(),
                google.email(),
                google.pictureUrl()
        );
    }
}
