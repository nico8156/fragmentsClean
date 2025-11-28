package com.nm.fragmentsclean.authContext.write.businesslogic.usecases;


import com.nm.fragmentsclean.authContext.write.businesslogic.gateways.IdentityRepository;
import com.nm.fragmentsclean.authContext.write.businesslogic.gateways.JwtTokenGenerator;
import com.nm.fragmentsclean.authContext.write.businesslogic.gateways.OAuthIdTokenVerifier;

import com.nm.fragmentsclean.authContext.write.businesslogic.models.AppSessionTokens;
import com.nm.fragmentsclean.authContext.write.businesslogic.models.Identity;
import com.nm.fragmentsclean.authContext.write.businesslogic.models.VerifiedOAuthProfile;
import com.nm.fragmentsclean.authContext.write.businesslogic.models.events.UserAuthenticatedEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.CommandHandlerWithResult;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;

import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DomainEventPublisher;
import com.nm.fragmentsclean.userContext.businesslogic.gateways.UserRepository;
import com.nm.fragmentsclean.userContext.businesslogic.models.AppUser;

import com.nm.fragmentsclean.userContext.businesslogic.readmodels.AppUserSnapshot;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;


@Transactional
public class RefreshSessionCommandHandler
        implements CommandHandlerWithResult<RefreshSessionCommand, RefreshSessionResult> {

    private final OAuthIdTokenVerifier oauthIdTokenVerifier;
    private final IdentityRepository identityRepository;
    private final UserRepository userRepository;
    private final JwtTokenGenerator jwtTokenGenerator;
    private final DateTimeProvider dateTimeProvider;
    private final DomainEventPublisher domainEventPublisher;


    public RefreshSessionCommandHandler(
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
    public RefreshSessionResult execute(RefreshSessionCommand cmd) {

        // 1. Vérifier l'idToken auprès du provider (Google)
        VerifiedOAuthProfile verified = oauthIdTokenVerifier.verify(
                cmd.provider(),
                cmd.idToken()
        );

        var now = dateTimeProvider.now();

        // 2. Identity : trouver ou créer
        var existingIdentityOpt = identityRepository
                .findByProviderAndProviderUserId(cmd.provider(), verified.providerUserId());

        Identity identity;
        AppUser user;

        if (existingIdentityOpt.isPresent()) {
            identity = existingIdentityOpt.get();
            user = userRepository
                    .findById(identity.userId())
                    .orElseGet(() -> {
                        // Cas rare: identity sans user → recréer user cohérent
                        var newUser = AppUser.createNewFromOAuthProfile(
                                verified.displayName(),
                                verified.avatarUrl(),
                                verified.locale(),
                                now
                        );
                        return userRepository.save(newUser);
                    });

        } else {
            // Pas d'identity → créer user + identity
            user = AppUser.createNewFromOAuthProfile(
                    verified.displayName(),
                    verified.avatarUrl(),
                    verified.locale(),
                    now
            );
            user = userRepository.save(user);

            identity = Identity.createNew(verified, user.id(), now);
            identity = identityRepository.save(identity);
        }

        // 3. Mettre à jour "dernière auth"
        identity.markAuthenticatedAt(now);
        identityRepository.save(identity);

        user.markAuthenticated(now);
        userRepository.save(user);

        // 4. Récupérer toutes les identities de ce user pour le snapshot
        List<Identity> allIdentitiesForUser = identityRepository.listByUserId(user.id());

        // 5. Générer le token applicatif (JWT)
        AppSessionTokens appTokens = jwtTokenGenerator.generateAccessToken(user, identity, now);

        // 6. Snapshot complet pour le read model / HTTP
        AppUserSnapshot userSnapshot = user.toSnapshot(allIdentitiesForUser);

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
                userSnapshot,
                appTokens,
                cmd.provider(),
                cmd.scopes(),
                nowResult
        );
    }
}
