package com.nm.fragmentsclean.userApplicationContext.write.businesslogic.usecases;

import com.nm.fragmentsclean.authenticationContext.write.businesslogic.models.AuthUserCreatedEvent;
import com.nm.fragmentsclean.sharedKernel.businesslogic.models.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
public class AuthUserCreatedEventHandler implements EventHandler<AuthUserCreatedEvent> {

    private static final Logger log = LoggerFactory.getLogger(AuthUserCreatedEventHandler.class);

    @Override
    public void handle(AuthUserCreatedEvent event) {
        // ✅ GoogleLoginCommandHandler est l’unique point de création AppUser.
        // Ce handler peut rester pour debug / métriques / future sync.
        log.info("AuthUserCreatedEvent received (no AppUser creation here). authUserId={} email={}",
                event.authUserId(), event.email());
    }
}
