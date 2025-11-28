package com.nm.fragmentsclean.authContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.authContext.write.businesslogic.gateways.IdentityRepository;
import com.nm.fragmentsclean.authContext.write.businesslogic.gateways.JwtTokenGenerator;
import com.nm.fragmentsclean.authContext.write.businesslogic.gateways.OAuthIdTokenVerifier;
import com.nm.fragmentsclean.authContext.write.businesslogic.models.Identity;
import com.nm.fragmentsclean.authContext.write.businesslogic.models.events.UserAuthenticatedEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.CommandHandlerWithResult;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.userContext.businesslogic.gateways.UserRepository;
import com.nm.fragmentsclean.userContext.businesslogic.models.AppUser;

import java.util.UUID;

public class LoginCommandHandler
        implements CommandHandlerWithResult<LoginCommand, RefreshSessionResult> {

    private final OAuthIdTokenVerifier oauthIdTokenVerifier;
    private final IdentityRepository identityRepository;
    private final UserRepository userRepository;
    private final JwtTokenGenerator jwtTokenGenerator;
    private final DateTimeProvider dateTimeProvider;
    private final DomainEventPublisher domainEventPublisher;   // 👈


    public LoginCommandHandler(
            OAuthIdTokenVerifier oauthIdTokenVerifier,
            IdentityRepository identityRepository,
            UserRepository userRepository,
            JwtTokenGenerator jwtTokenGenerator,
            DateTimeProvider dateTimeProvider,
            DomainEventPublisher domainEventPublisher
    ) {
        this.oauthIdTokenVerifier = oauthIdTokenVerifier;
        this.identityRepository = identityRepository;
        this.userRepository = userRepository;
        this.jwtTokenGenerator = jwtTokenGenerator;
        this.dateTimeProvider = dateTimeProvider;
        this.domainEventPublisher = domainEventPublisher;
    }

    @Override
    public RefreshSessionResult execute(LoginCommand cmd) {

        var now = dateTimeProvider.now();
        var verified = oauthIdTokenVerifier.verify(cmd.provider(), cmd.idToken());

        // Toujours créer un nouvel utilisateur
        var user = AppUser.createNewFromOAuthProfile(
                verified.displayName(),
                verified.avatarUrl(),
                verified.locale(),
                now
        );
        user = userRepository.save(user);

        // Toujours créer une identity liée
        var identity = Identity.createNew(verified, user.id(), now);
        identity = identityRepository.save(identity);

        // Mettre à jour “lastAuthAt”
        identity.markAuthenticatedAt(now);
        identityRepository.save(identity);

        user.markAuthenticated(now);
        userRepository.save(user);

        // Snapshot
        var identities = identityRepository.listByUserId(user.id());
        var snapshot = user.toSnapshot(identities);

        // Tokens
        var tokens = jwtTokenGenerator.generateAccessToken(user, identity, now);

        var nowResult = dateTimeProvider.now();

        domainEventPublisher.publish(
                new UserAuthenticatedEvent(
                        UUID.randomUUID(),   // eventId
                        now,                 // occurredAt
                        user.id(),
                        cmd.provider()
                )
        );

        return new RefreshSessionResult(
                snapshot,
                tokens,
                cmd.provider(),
                cmd.scopes(),
                nowResult
        );
    }
}
