package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthUserCreatedEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.DateTimeProvider;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.gateways.AppUserRepository;
import com.nm.fragmentsclean.userApplicationContext.write.businesslogic.models.AppUser;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class AuthUserCreatedEventHandler implements EventHandler<AuthUserCreatedEvent> {

    private final AppUserRepository appUserRepository;
    private final DateTimeProvider dateTimeProvider;

    public AuthUserCreatedEventHandler(AppUserRepository appUserRepository,
                                       DateTimeProvider dateTimeProvider) {
        this.appUserRepository = appUserRepository;
        this.dateTimeProvider = dateTimeProvider;
    }

    @Override
    public void handle(AuthUserCreatedEvent event) {
        // Idempotent : si un AppUser existe déjà pour cet authUserId → on ne recrée pas
        appUserRepository.findByAuthUserId(event.authUserId())
                .ifPresentOrElse(
                        existing -> syncExisting(existing, event),
                        () -> createNewFromEvent(event)
                );
    }

    private void syncExisting(AppUser existing, AuthUserCreatedEvent event) {
        // Pour l’instant on ne synchronise rien.
        // Plus tard : existing.syncFromAuth(event.email(), ...); puis appUserRepository.save(existing);
    }

    private void createNewFromEvent(AuthUserCreatedEvent event) {
        Instant now = dateTimeProvider.now();

        String email = event.email();
        String localPart = email != null && email.contains("@")
                ? email.substring(0, email.indexOf('@'))
                : "user";

        String displayName = "User " + localPart;

        AppUser appUser = AppUser.createNew(
                event.authUserId(),
                displayName,
                null,
                now
        );

        appUserRepository.save(appUser);
    }
}
